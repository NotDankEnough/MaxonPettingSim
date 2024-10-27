package kz.ilotterytea.maxon.utils;

public class OsUtils {
    private static final String OS = System.getProperty("os.name").toLowerCase();

    static public boolean isAndroid = System.getProperty("java.runtime.name").contains("Android");
    static public boolean isMac = !isAndroid && OS.contains("mac");
    static public boolean isWindows = !isAndroid && OS.contains("windows");
    static public boolean isLinux = !isAndroid && OS.contains("linux");
    static public boolean isIos = !isAndroid && (!(isWindows || isLinux || isMac)) || OS.startsWith("ios");

    public static boolean isMobile = isIos || isAndroid;

    public static final boolean isPC = isWindows || isMac || isLinux;

    static {
        try {
            Class.forName("com.google.gwt.core.client.GWT");
        }
        catch(Exception ignored) { /* IGNORED */ }

        boolean isMOEiOS = "iOS".equals(System.getProperty("moe.platform.name"));
        if (isMOEiOS || (!isAndroid && !isWindows && !isLinux && !isMac)) {
            isIos = true;
            isAndroid = false;
            isWindows = false;
            isLinux = false;
            isMac = false;
            isMobile = true;
        }
    }

    public static String getUserDataDirectory(String applicationName)
    {
        String DATA_HOME;

        if((DATA_HOME = System.getenv("XDG_DATA_HOME"))==null)
        {
            if(isLinux || isAndroid)
            {
                DATA_HOME = System.getProperty("user.home")+"/.local/share";
            }
            else if(isMac)
            {
                DATA_HOME = System.getProperty("user.home")+"/Library/Application Support";
            }
            else if(isIos)
            {
                DATA_HOME = System.getProperty("user.home")+"/Documents";
            }
            else if(isWindows)
            {
                if((DATA_HOME = System.getenv("APPDATA"))==null)
                {
                    DATA_HOME = System.getProperty("user.home")+"/Documents/My Games";
                }
            }
        }

        if(applicationName==null || DATA_HOME==null) return DATA_HOME;

        return DATA_HOME+"/"+applicationName;
    }

}