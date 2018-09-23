package org.sushobh;

public class Util {

    public static String getPathAtWorkingDirectory(String name){
        return System.getProperty("user.dir")+"\\"+name;
    }

}
