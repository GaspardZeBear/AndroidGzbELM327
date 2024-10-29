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
    private String raw;
    private int pidVal;

    private static Map<String,String> pidHash;
    static {
        pidHash = new HashMap<>();
        pidHash.put("00","00!");
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
    public String getRaw() {
        return(raw);
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

    private boolean checkModeValue(String mode) {
        if (!mode.equals("41")) {
            Log.d("ELM327Response", "parseResponseData() no consistent mode , expected 41 got " + mode);
            return(false);
        }
        return(true);
    }

    private boolean checkEcuValue(String ecu) {
        if (!ecu.startsWith("7")) {
            Log.d("ELM327Response", "parseResponseData() ecu should begin with 7, got " + ecu);
            return (false);
        }
        return(true);
    }

    private boolean checkMsgLength(int target, int val) {
        if (val < target) {
            Log.d("ELM327Response", "parseResponseData() data too short < : got " + val + " vs " + target);
            return (false);
        }
        return (true);
    }

    public void parseResponseData(String str) {
        Log.d("ELM327Response", "parseResponseData(): " + str);
        // Eliminate all non hexa cars
        String data=getAsciiHexaString(str);
        //String data=getAsciiHexaString("010C\n\n7E8^J 03 41\n 0D 79 \n\n");
        while (data.startsWith("010C") || data.startsWith("010D")) {
            data=data.substring(4);
        }
        raw=data;
        // Data is clean, may contains some PID results (ex 7E803410D3F:7E804410C0344 (':' for readability only ! ))
        // .. here only process the first one
        // ate0 must have been sent
        // Simulator : 010C^J7E..
        // dokker : 7E8
        Log.d("ELM327Response", "parseResponseData(): data #" + data + "#");
        // A least <ECU(3 chars)><length of value(2 chars)><mode : '41' (2 chars)<value (x chars), min 2 chars >
        // Ex : 7E803410D3F : <7E8><03><41><0D><3F> : ECU 7E8, length 0, mode 41, pid 0D, value 3F
        // So minimum 9 chars !

        if (!checkMsgLength(9, data.length())) return;

        String mode=data.substring(5,7);
        if (!checkModeValue(mode)) return;

        String ecu=data.substring(0,3);
        if (!checkEcuValue(ecu)) return;

        pid = data.substring(7,9);

        String valLen=data.substring(3,5);
        int len=2*computePidVal(valLen)-4;
        if (!checkMsgLength(len+9,data.length())) return;

        strPidVal = data.substring(9, len+9);
        Log.d("ELM327Response", "parseResponseData() will analyze  " + data + " ecu " + ecu + " len " + len + " strPidVal : " + strPidVal);
        pidVal = computePidVal(strPidVal);
        pidAlias = ELM327Response.pidHash.get(pid);
        Log.d("ELM327Response", "strPidVal: <"+strPidVal+">"+" pidVal <"+ String.valueOf(pidVal)+">");
    }

}
