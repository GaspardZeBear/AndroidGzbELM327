package com.gzb.elm327;

import android.util.Log;

import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;

public class ELM327Response {

    private String pid;
    private String pidAlias;
    private String strPidVal;
    private int pidVal;

    private static Map<String,String> pidHash;
    static {
        pidHash = new HashMap<>();
        pidHash.put("0D","SPEED");
        pidHash.put("0C","RPM");
    }

    public ELM327Response(String str) {
        pidAlias="UNKNOWN";
        strPidVal="-1";
        pidVal=-1;
        this.parseResponseData(str) ;
    }


    public String getPid() {
        return(pid);
    }

    public String getPidAlias() {
        return(pidAlias);
    }

    public int getPidVal() {
        return(pidVal);
    }

    public String getStrPidVal() {
        return(strPidVal);
    }

    public int getCharPos(char[] tab, int val,int defaultPos) {
        int pos=defaultPos;
        for (int i = 0; i < tab.length ; i++) {
            if (tab[i] == (int)val) {
                pos=i;
                break;
            }
        };
        return(pos);
    }


    public String getAsciiHexaString(String str) {
        char[] charArray = str.toCharArray();

        StringBuilder dump = new StringBuilder();
        for (int i = 0; i < charArray.length ; i++) {
            dump.append(Integer.toHexString((int)charArray[i]));
        }
        Log.d("Convert", "DUMP str #" + str + "#");
        Log.d("Convert", "DUMP hex #" + dump.toString() + "#");

        StringBuilder clean = new StringBuilder();
        for (int i = 0; i < charArray.length ; i++) {
            try {
                int intValue = charArray[i];
                // Remove non hexa chars
                if ( (intValue >= 48 && intValue < 58) || (intValue >= 65 && intValue < 71) ) {
                    clean.append(Character.toChars(intValue));
                }
            }
            catch (NumberFormatException nfe) {
                Log.d("Convert", "NumberFormatException: " + nfe.getMessage());
            }
        }
        return clean.toString();
    }


    public int computePidVal(String str) {
        Log.d("ELM327Response", "computePidVal(): str " + str);
        char[] ch = str.toCharArray();
        int power=ch.length;
        int total=0;
        for (char c : ch) {
            //Log.d("ELM327Response", "computePidVal(): c " + c);
            int a = Character.getNumericValue(c)*(int)Math.pow(16,power-1);
            total += a;
            power--;
        //System.out.println(c + " int value: " + a + " total " + total);
        }
        Log.d("ELM327Response", "computePidVal(): total " + total);
        return(total);
    }

    public void parseResponseData(String str) {
        Log.d("ELM327Response", "parseResponseData(): " + str);
        String data="";
        String ecu="";
        int offset=0;
        if ( str.startsWith("010C^J010C") || str.startsWith("010D^J010D") ) {
            Log.d("ELM327Response", "parseResponseData(): msg identified ");
            offset="010C^J010C".length();
            pid = str.substring(2, 4);
            if (str.length() > offset) {
                data = getAsciiHexaString(str.substring(offset + 1));
            }
            //ecu=data.substring(offset,offset+4);
        } else if ( str.startsWith("7E")) {
            //ecu=str.substring(0,4);
            data=getAsciiHexaString(str);
            pid = data.substring(6,8);
        }
        Log.d("ELM327Response", "parseResponseData(): data #" + data + "#");
        if ( data.length() > 8 ) {
            ecu=data.substring(0,3);
            pid = data.substring(7,9);
            strPidVal = data.substring(9);
            Log.d("ELM327Response", "parseResponseData() will analyze  " + data + " ecu " + ecu +  " strPidVal : " + strPidVal);
            pidVal=computePidVal(strPidVal);
            pidAlias=ELM327Response.pidHash.get(pid);
        } else {
            Log.d("ELM327Response", "parseResponseData() cannot analyze  " + str);
        }
        Log.d("ELM327Response", "strPidVal: <"+strPidVal+">"+" pidVal <"+ String.valueOf(pidVal)+">");
    }


}
