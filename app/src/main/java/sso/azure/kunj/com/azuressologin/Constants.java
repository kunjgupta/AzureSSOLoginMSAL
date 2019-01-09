package sso.azure.kunj.com.azuressologin;

public class Constants {

    public static final String TAG = "AzureSSOPOC";

    //You'll get below info from Azure AD Application configurations
    public static final String REDIRECT_BASE_URL = "Please enter here your redirect url";
    public static final String CLIENT_ID = "Please enter here your client id";//Format - xxxxxxxx-xxxx-xxxx-xxxx-xxxxxxxxxxxx
    public final static String SCOPES [] = {"https://graph.microsoft.com/User.Read"};

    public static final String PREFS_ACCESS_TOKEN = "access_token";
}
