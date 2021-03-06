package tm.salam.gpstracker.service;

import com.fazecast.jSerialComm.SerialPort;
import com.fazecast.jSerialComm.SerialPortDataListener;
import com.fazecast.jSerialComm.SerialPortEvent;
import lombok.Data;
import org.springframework.stereotype.Component;
import org.springframework.stereotype.Service;

import java.util.ArrayList;

@Service
public class GsmServiceImpl implements GsmService{

    private SerialPort serialPort;
    private String result;
    public static String[] SmsStorage = new String[]{"MT", "SM"};

    @Override
    public String[] getSmsStorage(){

        return SmsStorage;
    }

    @Override
    public String executeAT(String at, int waitingTime) {
        at = at + "\r\n";
        result = "";
        int i = 0;
        byte[] bytes = at.getBytes();
        serialPort.writeBytes(bytes, bytes.length);
        while ((result.trim().equals("") || result.trim().equals("\n")) && i < waitingTime) {
            try {
                i++;
                Thread.sleep(500);
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        }

        return result;
    }

    @Override
    public String executeUSSD(String ussd) {
//        executeAT("AT+CUSD=1", 1);
        String cmd = "AT+CUSD=1,\"" + ussd + "\",15";
        result = "";
        // serialPort.writeBytes((cmd).getBytes(), cmd.getBytes().length);
        executeAT(cmd, 2);
        if (result.contains("ERROR")) {
//            System.out.println("USSD error");
            return result;
        }
        String str = "";
        result = "";
        int waiting = 0;
        while ((result.trim().equals("") || result.trim().equals("\n")) && waiting < 10) {
            try {
                waiting++;
                Thread.sleep(1000);
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        }
        if (result.contains("+CUSD")) {
            str = result.substring(12, result.length() - 6);
            // System.out.println(result.substring(12, result.length() - 6));
            // for huawei e173 just return the pure result, no need for extra treatment
//            String[] arr = str.split("(?<=\\G....)");
//            Iterable<String> arr = Splitter.fixedLength(4).split(str);
//            str = "";
//            for (String s : arr) {
//                int hexVal = Integer.parseInt(s, 16);
//                str += (char) hexVal;
//            }
        }
        return str;
    }

    @Override
    public ArrayList<String[]> readSms() {
        executeAT("ATE0", 1);
        executeAT("AT+CSCS=\"GSM\"", 1);
        executeAT("AT+CMGF=1", 1);
        ArrayList<String[]>sms=new ArrayList<>();

        for (String value : SmsStorage) {
            executeAT("AT+CPMS=\"" + value + "\"", 1);
            executeAT("AT+CMGL=\"ALL\"", 5);
            if (result.contains("+CMGL")) {
                String[] strs = result.replace("\"", "").split("(?:,)|(?:\r\n)");
//                Sms sms;
//                for (int i = 1; i < strs.length - 1; i++) {
//                    sms = new Sms();
//                    sms.setId(Integer.parseInt(strs[i].charAt(strs[i].length() - 1) + ""));
//                    sms.setStorage(value);
//                    i++;
//                    sms.setStatus(strs[i]);
//                    i++;
//                    sms.setPhone_num(strs[i]);
//                    i++;
//                    sms.setPhone_name(strs[i]);
//                    i++;
//                    sms.setDate(strs[i]);
//                    i++;
//                    sms.setTime(strs[i]);
//                    i++;
//                    if (Longs.tryParse(strs[i].substring(0, 2)) != null) { //get the message UNICODE
//                        Iterable<String> arr = Splitter.fixedLength(4).split(strs[i]);
//                        StringBuilder con = new StringBuilder();
//                        for (String s : arr) {
//                            int hexVal = Integer.parseInt(s, 16);
//                            con.append((char) hexVal);
//                        }
//                        sms.setContent(con.toString());
//                    } else {//get the message String
//                        sms.setContent(strs[i]);
//                    }
//                    if (!strs[i + 1].equals("") && !strs[i + 1].startsWith("+")) {
//                        i++;
//                        sms.setContent(sms.getContent() + "\n" + strs[i]);
//                        i++;
//                    }
//                    str.add(sms);
//                    if (strs[i + 1].equals("") && strs[i + 2].equals("OK")) {
//                        break;
//                    }
//                }
            sms.add(strs);
            }
        }
        return sms;
    }

    @Override
    public String sendSms(String num, String sms) {
        executeAT("ATE0", 1);
        executeAT("AT+CSCS=\"GSM\"", 1);
        executeAT("AT+CMGF=1", 1);
        executeAT("AT+CMGS=\"" + num + "\"", 2);
        executeAT(sms, 2);
        executeAT(Character.toString((char) 26), 10);
//        System.out.println(result);
        return result;
    }

    @Override
    public String deleteSms(int smsId, String storage) {
        executeAT("AT+CPMS=\"" + storage + "\"", 1);
        executeAT("AT+CMGD=" + smsId, 1);
        return result;
    }

    @Override
    public String deleteAllSms(String storage) {
        executeAT("AT+CPMS=\"" + storage + "\"", 1);
        executeAT("AT+CMGD=0, 4", 1);
        return result;
    }

    @Override
    public boolean initialize(String port) {
        serialPort = SerialPort.getCommPort(port);
        if (serialPort.openPort()) {
            serialPort.addDataListener(new SerialPortDataListener() {
                @Override
                public int getListeningEvents() {
                    return SerialPort.LISTENING_EVENT_DATA_AVAILABLE;
                }

                @Override
                public void serialEvent(SerialPortEvent event) {
                    byte[] msg = new byte[serialPort.bytesAvailable()];
                    serialPort.readBytes(msg, msg.length);
                    //System.out.println(res);
                    result = new String(msg);
                }
            });
            // Prepare for ussd
//            executeAT("AT^USSDMODE=0", 1);
//            if (result.equals(""))
//                return false;
            // turn off periodic status messages (RSSI status, etc.)
//            executeAT("AT^CURC=0", 1);
            return true;
        } else {
            return false;
        }

    }

    @Override
    public String[] getSystemPorts() {
        String[] systemPorts = new String[SerialPort.getCommPorts().length];
        for (int i = 0; i < systemPorts.length; i++) {
            systemPorts[i] = SerialPort.getCommPorts()[i].getSystemPortName();
        }
        return systemPorts;
    }

    @Override
    public boolean closePort() {
        if (serialPort != null)
            return serialPort.closePort();
        else
            return true;
    }

}
