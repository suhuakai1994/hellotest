package com.huawei.oss.at.corba.session;

import com.huawei.oss.at.corba.common.Utils;
import com.huawei.oss.at.corba.exception.CorbaRequestAbnormalException;
import javafx.util.Pair;
import org.apache.http.*;
import org.apache.http.client.config.RequestConfig;
import org.apache.http.client.entity.UrlEncodedFormEntity;
import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.conn.ssl.SSLConnectionSocketFactory;
import org.apache.http.conn.ssl.SSLContextBuilder;
import org.apache.http.conn.ssl.TrustStrategy;
import org.apache.http.conn.ssl.X509HostnameVerifier;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.DefaultRedirectStrategy;
import org.apache.http.impl.client.HttpClientBuilder;
import org.apache.http.impl.client.HttpClients;
import org.apache.http.message.BasicNameValuePair;
import org.apache.http.protocol.HttpContext;
import org.apache.http.util.EntityUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.net.ssl.SSLContext;
import javax.net.ssl.SSLException;
import javax.net.ssl.SSLSession;
import javax.net.ssl.SSLSocket;
import java.io.IOException;
import java.net.URI;
import java.net.URLEncoder;
import java.security.GeneralSecurityException;
import java.security.cert.CertificateException;
import java.security.cert.X509Certificate;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.List;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.locks.ReadWriteLock;
import java.util.concurrent.locks.ReentrantReadWriteLock;

import static org.apache.http.client.config.RequestConfig.custom;

public class SessionManager {

    public static final SessionManager getInstance() {
        return TokenManagerHolder.INSTANCE;
    }

    public String getUserSession(LoginConfig config) {
        logger.info("begin get user session");

        String key = Utils.concantTwoString(config.getLoginHost(), config.getUserName());
        locker.readLock().lock();
        try {
            // return user session after try to find in cache if key exists and session not expired
            if (findUserSessionInCache(key, new Date())) {
                logger.info("user session found, key {}", key);
                return sessionCache.get(key).getKey();
            }
        } finally {
            locker.readLock().unlock();
        }

        locker.writeLock().lock();
        try {
            // return user session if other thread has obtained cache and session not expired
            if (findUserSessionInCache(key, new Date())) {
                logger.info("user session found after lock, key {}", key);
                return sessionCache.get(key).getKey();
            }

            // session should be expired after double check
            if (sessionCache.containsKey(key)) {
                logger.info("user session found expired, key {}", key);
                logOutUserSession(config.getUserName(), config.getLoginHost(), sessionCache.get(key).getKey());
                sessionCache.remove(key);
            }

            // login user to get session token
            String session = loginUser(config.getLoginHost(), config.getUserName(), config.getPassword());
            if (session.isEmpty()) {
                throw new CorbaRequestAbnormalException("get empty user session");
            }
            Calendar c = Calendar.getInstance();
            c.setTime(new Date());
            c.add(Calendar.MINUTE, 20);
            // cache user session and set expire after 20 mins
            sessionCache.put(key, new Pair<>(session, c.getTime()));
            logger.info("get user session successfully, key {}", key);

            return session;
        } catch (Exception ex) {
            logger.info("get user session catch exception", ex);
        } finally {
            locker.writeLock().unlock();
        }

        throw new CorbaRequestAbnormalException("get user session failed");
    }

    private void logOutUserSession(String userName, String hostIp, String sessionCookie) {
        logger.info("logout user session, userName {} hostIp {} session {}", userName, hostIp, sessionCookie);

        GetSessionCookieRedirectStrategy redirectStrategy = new GetSessionCookieRedirectStrategy();
        HttpClientBuilder clientBuilder = HttpClients.custom().setRedirectStrategy(redirectStrategy)
                .setSSLSocketFactory(createSSLConnSocketFactory());
        RequestConfig.Builder requestConfigBuilder = custom();
        requestConfigBuilder.setCircularRedirectsAllowed(true);

        try (CloseableHttpClient client = clientBuilder.build()) {
            HttpGet get = new HttpGet();
            get.setConfig(requestConfigBuilder.build());
            String logoutUrl = String.format("https://%s/unisess/v1/logout?service=%%2Fadminhomewebsite%%2Findex.html", hostIp);
            get.setURI(URI.create(logoutUrl));
            get.addHeader("Cookie", String.format("bspsession=%s", sessionCookie));
            client.execute(get);
        } catch (Exception ex) {
            logger.error("logout user catch exception", ex);
        }
    }

    private boolean findUserSessionInCache(String key, Date currentDate) {
        if (sessionCache.containsKey(key)) {
            Date expiredDate = sessionCache.get(key).getValue();
            return currentDate.before(expiredDate);
        }

        return false;
    }

    private String loginUser(String hostIp, String userName, String password) {
        logger.info("login user, userName {} hostIp {}", userName, hostIp);

        String token = "";
        GetSessionCookieRedirectStrategy redirectStrategy = new GetSessionCookieRedirectStrategy();
        HttpClientBuilder clientBuilder = HttpClients.custom().setRedirectStrategy(redirectStrategy)
                .setSSLSocketFactory(createSSLConnSocketFactory());

        try (CloseableHttpClient client = clientBuilder.build()) {
            RequestConfig.Builder requestConfigBuilder = RequestConfig.custom();
            requestConfigBuilder.setCircularRedirectsAllowed(true);

            HttpGet get = new HttpGet();
            get.setConfig(requestConfigBuilder.build());
            get.setURI(URI.create(String.format("https://%s", hostIp)));
            try (CloseableHttpResponse response = client.execute(get)) {
                String location = redirectStrategy.location;
                if (location.equals("") || !location.contains("?")) {
                    logger.error(
                            "login user get redirect location failed " + EntityUtils.toString(response.getEntity()));
                    return token;
                }
            } finally {
                get.releaseConnection();
            }

            HttpPost post = new HttpPost();
            post.setConfig(requestConfigBuilder.build());
            String queryParam = redirectStrategy.location.split("\\?")[1];
            List<NameValuePair> nvps = new ArrayList<NameValuePair>();
            String hostUrl = String.format("https://%s/unisso/validateUser.action?", hostIp);
            post.setURI(URI.create(hostUrl + queryParam));
            nvps.add(new BasicNameValuePair("userpasswordcredentials.username", userName));
            nvps.add(new BasicNameValuePair("userpasswordcredentials.password", password));
            nvps.add(new BasicNameValuePair("__checkbox_warnCheck", "true"));
            nvps.add(new BasicNameValuePair("Submit", "Login"));

            post.setEntity(new UrlEncodedFormEntity(nvps));
            post.addHeader("Content-Type", "application/x-www-form-urlencoded");

            try (CloseableHttpResponse response = client.execute(post)) {
                if (redirectStrategy.session.equals("")) {
                    logger.error("login user get session failed " + EntityUtils.toString(response.getEntity()));
                    return token;
                }
            } finally {
                post.releaseConnection();
            }

            String sessionCookie = redirectStrategy.session;
            logger.debug("login user session cookie {}", sessionCookie);

            if (redirectStrategy.isLicenselogin) {
                String redirectUrlParam = URLEncoder.encode("service=/unisess/v1/auth?service=/", "UTF-8");
                String licenseRedirectloginUrl = String.format("https://%s/plat/licapp/v1/licensedirectlogin?%s", hostIp, redirectUrlParam);
                get.setURI(URI.create(licenseRedirectloginUrl));
                get.addHeader("X_Requested_With", "XMLHttpRequest");
                get.addHeader("Cookie", sessionCookie);
                try (CloseableHttpResponse response = client.execute(get)) {
                    String licenseLoginPageRes = EntityUtils.toString(response.getEntity());
                    logger.info("login user licenselogin {}", licenseLoginPageRes);
                    int statusCode = response.getStatusLine().getStatusCode();
                    if (statusCode != 200 || licenseLoginPageRes.contains("system.error")) {
                        logger.error("login user licenselogin failed");
                        return "";
                    }
                } finally {
                    get.releaseConnection();
                }
                get.setURI(URI.create(String.format("https://%s/adminhomewebsite/index.html", hostIp)));
                try (CloseableHttpResponse response = client.execute(get)) {
                } finally {
                    get.releaseConnection();
                }
            }

            token = sessionCookie.split("=")[1];
            logger.info("login user successfully, userName {} hostIp {} token {}", userName, hostIp, token);
        } catch (Exception e) {
            String errorMsg = e.getMessage();
            if (errorMsg.contains("loginformverify")) {
                logger.error("login user verity code is required");
            }
            logger.error("login user catch exception", e);
        }

        return token;
    }

    private SSLConnectionSocketFactory createSSLConnSocketFactory() {
        SSLConnectionSocketFactory sslsf = null;
        try {
            SSLContext sslContext = new SSLContextBuilder().loadTrustMaterial(null, new TrustStrategy() {
                public boolean isTrusted(X509Certificate[] chain, String authType) throws CertificateException {
                    return true;
                }
            }).build();
            sslsf = new SSLConnectionSocketFactory(sslContext, new X509HostnameVerifier() {

                @Override
                public boolean verify(String arg0, SSLSession arg1) {
                    return true;
                }

                @Override
                public void verify(String host, SSLSocket ssl) throws IOException {
                }

                @Override
                public void verify(String host, X509Certificate cert) throws SSLException {
                }

                @Override
                public void verify(String host, String[] cns, String[] subjectAlts) throws SSLException {
                }
            });
        } catch (GeneralSecurityException e) {
            logger.error("get session cookie ssl exception", e);
        }
        return sslsf;
    }

    private class GetSessionCookieRedirectStrategy extends DefaultRedirectStrategy {

        public String session = "";

        public Boolean isLicenselogin = false;

        public String location = "";

        @Override
        public boolean isRedirected(HttpRequest request, HttpResponse response, HttpContext context) {
            boolean isRedirect = false;
            try {
                isRedirect = super.isRedirected(request, response, context);
                Header[] requestheaders = response.getAllHeaders();
                for (Header header : requestheaders) {
                    logger.trace("get session cookie redirect header name {} value {}", header.getName(),
                            header.getValue());
                    if (header.getName().equals("Set-Cookie") && header.getValue().contains("bspsession=")) {
                        session = header.getValue().split(";")[0];
                    }
                    if (header.getName().equals("Location")) {
                        location = header.getValue();
                        if (location.contains("/plat/licapp/v1/themes/default/licenseChooseMenu.html")) {
                            isLicenselogin = true;
                        }
                    }
                }
            } catch (ProtocolException e) {
                logger.error("get session cookie redirect catch exception", e);
            }
            if (!isRedirect) {
                int responseCode = response.getStatusLine().getStatusCode();
                if (responseCode == 301 || responseCode == 302) {
                    return true;
                }
            }
            return isRedirect;
        }
    }

    private static class TokenManagerHolder {
        private static final SessionManager INSTANCE = new SessionManager();
    }

    private SessionManager() {
        sessionCache = new ConcurrentHashMap<>();
    }

    private ConcurrentHashMap<String, Pair<String, Date>> sessionCache;

    private ReadWriteLock locker = new ReentrantReadWriteLock();

    private static Logger logger = LoggerFactory.getLogger(SessionManager.class);
}
