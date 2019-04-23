package com.huawei.oss.at.corba.common;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.text.SimpleDateFormat;
import java.util.Date;

public class Utils {

    public static String getCurrentTime() {
        return new SimpleDateFormat("yyyy-MM-dd HH:mm:ss.SSSS").format(new Date());
    }

    public static byte[] getUserToken(String sessionId) {
        try {
            return sessionId.getBytes("ISO-8859-1");
        } catch (Exception ex) {
            logger.error("getUserSession catch exception", ex);
            return new byte[0];
        }
    }

    public static String concantTwoString(String firstOne, String secondOne) {
        return String.format("%s_%s", firstOne, secondOne);
    }

    public static boolean isEmpty(String word) {
        return word == null || "".equals(word.trim());
    }

    public static Integer tryParseInt(String value) {
        Integer retVal;
        try {
            retVal = Integer.parseInt(value);
        } catch (NumberFormatException nfe) {
            logger.error("try to parse {} to int catch exception", value, nfe);
            retVal = null;
        }
        return retVal;
    }

    private static Logger logger = LoggerFactory.getLogger(Utils.class);
}
