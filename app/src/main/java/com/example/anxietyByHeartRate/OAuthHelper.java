//package com.example.anxietyByHeartRate;
//
//import com.google.api.client.auth.oauth.OAuthAuthorizeTemporaryTokenUrl;
//import com.google.api.client.auth.oauth.OAuthCredentialsResponse;
//import com.google.api.client.auth.oauth.OAuthGetAccessToken;
//import com.google.api.client.auth.oauth.OAuthGetTemporaryToken;
//import com.google.api.client.auth.oauth.OAuthHmacSigner;
//import com.google.api.client.http.HttpTransport;
//import com.google.api.client.http.javanet.NetHttpTransport;
//import com.google.api.client.json.JsonFactory;
//import com.google.api.client.json.jackson2.JacksonFactory;
//
//import java.io.IOException;
//
//public class OAuthHelper {
//    private static final String REQUEST_TOKEN_URL = "https://connectapi.garmin.com/oauth-service/oauth/request_token";
//    private static final String AUTHORIZE_URL = "https://connect.garmin.com/oauthConfirm";
//    private static final String ACCESS_TOKEN_URL = "https://connectapi.garmin.com/oauth-service/oauth/access_token" ;
//
//    private static final String CONSUMER_KEY = "1f52cb42-4642-4ed0-8915-04347a549d68";
//    private static final String CONSUMER_SECRET = "zx7Wlh4sxWCWnR0o5mUf67SAIZ4KidWSV4M";
//
//    private final HttpTransport transport;
//    private final JsonFactory jsonFactory;
//
//    public OAuthHelper() {
//        transport = new NetHttpTransport();
//        jsonFactory = new JacksonFactory();
//    }
//
//    public OAuthCredentialsResponse getTemporaryToken() throws IOException {
//        OAuthHmacSigner signer = new OAuthHmacSigner();
//        signer.clientSharedSecret = CONSUMER_SECRET;
//
//        OAuthGetTemporaryToken requestToken = new OAuthGetTemporaryToken(REQUEST_TOKEN_URL);
//        requestToken.consumerKey = CONSUMER_KEY;
//        requestToken.transport = transport;
//        requestToken.signer = signer;
//        requestToken.callback = null;
//
//        return requestToken.execute();
//    }
//
//    public String getAuthorizationUrl(String temporaryToken) {
//        OAuthAuthorizeTemporaryTokenUrl authorizeUrl = new OAuthAuthorizeTemporaryTokenUrl(AUTHORIZE_URL);
//        authorizeUrl.temporaryToken = temporaryToken;
//        return authorizeUrl.build();
//    }
//
//    public OAuthCredentialsResponse getAccessToken(String temporaryToken, String verifier) throws IOException {
//        OAuthHmacSigner signer = new OAuthHmacSigner();
//        signer.clientSharedSecret = CONSUMER_SECRET;
//
//        OAuthGetAccessToken accessToken = new OAuthGetAccessToken(ACCESS_TOKEN_URL);
//        accessToken.consumerKey = CONSUMER_KEY;
//        accessToken.signer = signer;
//        accessToken.transport = transport;
//        accessToken.temporaryToken = temporaryToken;
//        accessToken.verifier = verifier;
//
//        return accessToken.execute();
//    }
//}
//
