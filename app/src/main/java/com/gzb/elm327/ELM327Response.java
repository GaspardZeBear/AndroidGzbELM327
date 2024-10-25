package com.gzb.elm327;

import android.util.Log;

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
        strPidVal="?";
        strPidVal="-1";
        pidVal=-1;
        this.parseResponseData(str) ;

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
        StringBuilder clean = new StringBuilder();
        char[] charArray = str.toCharArray();
        // Find response : between "LF" and ">" (elm327 prompt)
        int lfPos=getCharPos(charArray,0x0A,0);
        int gtPos=getCharPos(charArray,0x3e, charArray.length);
        for (int i = lfPos; i < gtPos ; i++) {
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

    public int computePidVal(String str) {
        Log.d("ELM327Response", "computePidVal(): str " + str);
        char[] ch = str.toCharArray();
        int power=ch.length;
        int total=0;
        for (char c : ch) {
            Log.d("ELM327Response", "computePidVal(): c " + c);
            int a = Character.getNumericValue(c)*(int)Math.pow(16,power-1);
            total += a;
            power--;
        //System.out.println(c + " int value: " + a + " total " + total);
        }
        Log.d("ELM327Response", "computePidVal(): total " + total);
        return(total);
    }


    public void parseResponseData(String str) {
        String data=getAsciiHexaString(str);
        Log.d("ELM327Response", "parseResponseData(): " + data);
        if (data.length() > 2  && data.startsWith("41")) {
            Log.d("ELM327Response", "parseResponseData(): 41 found");
            pid = data.substring(2,4);
            for (Map.Entry<String, String> entry : ELM327Response.pidHash.entrySet()) {
                Log.d("ELM327Response","map " + entry.getKey() + ": " + entry.getValue());
            }
            Log.d("ELM327Response", "parseResponseData(): pid " + pid + " OC : <" + ELM327Response.pidHash.get("0C") + ">");
            if (ELM327Response.pidHash.containsKey(pid)) {
                pidAlias=ELM327Response.pidHash.get(pid);
            } else {
                pidAlias="UNKOWN";
            }
            strPidVal = data.substring(4);
            pidVal=computePidVal(strPidVal);
        }
        //else {
        //    pid="?";
        //}
        //pidVal=110;
        //return(data);
    }


}
