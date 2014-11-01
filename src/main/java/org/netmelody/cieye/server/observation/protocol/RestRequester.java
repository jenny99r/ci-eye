package org.netmelody.cieye.server.observation.protocol;

import org.apache.http.Header;
import org.apache.http.HttpEntity;
import org.apache.http.HttpHost;
import org.apache.http.HttpResponse;
import org.apache.http.auth.AuthScheme;
import org.apache.http.auth.AuthScope;
import org.apache.http.auth.UsernamePasswordCredentials;
import org.apache.http.client.AuthCache;
import org.apache.http.client.CredentialsProvider;
import org.apache.http.client.HttpResponseException;
import org.apache.http.client.ResponseHandler;
import org.apache.http.client.config.RequestConfig;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.client.methods.HttpPut;
import org.apache.http.client.protocol.HttpClientContext;
import org.apache.http.config.Registry;
import org.apache.http.config.RegistryBuilder;
import org.apache.http.conn.socket.ConnectionSocketFactory;
import org.apache.http.conn.ssl.SSLConnectionSocketFactory;
import org.apache.http.conn.ssl.SSLContextBuilder;
import org.apache.http.conn.ssl.SSLContexts;
import org.apache.http.conn.ssl.TrustStrategy;
import org.apache.http.entity.StringEntity;
import org.apache.http.impl.auth.BasicScheme;
import org.apache.http.impl.client.BasicCredentialsProvider;
import org.apache.http.impl.client.BasicResponseHandler;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClients;
import org.apache.http.impl.conn.PoolingHttpClientConnectionManager;
import org.apache.http.message.BasicHeader;
import org.apache.http.util.EntityUtils;
import org.netmelody.cieye.core.logging.LogKeeper;
import org.netmelody.cieye.core.logging.Logbook;

import javax.net.ssl.SSLContext;
import java.io.IOException;
import java.security.KeyManagementException;
import java.security.KeyStoreException;
import java.security.NoSuchAlgorithmException;
import java.security.cert.CertificateException;
import java.security.cert.X509Certificate;
import java.util.ArrayList;
import java.util.Collection;

public final class RestRequester implements GrapeVine {

    private static final Logbook LOG = LogKeeper.logbookFor(RestRequester.class);

    private final boolean privileged;
    private final CloseableHttpClient client;
    private final HttpClientContext context;

    public RestRequester(String username, String password) {
        privileged = !username.isEmpty();

        final PoolingHttpClientConnectionManager connManager = new PoolingHttpClientConnectionManager(getSocketFactoryRegistry());
        connManager.setMaxTotal(200);
        connManager.setDefaultMaxPerRoute(20);

        RequestConfig requestConfig = RequestConfig.custom().setSocketTimeout(30000).build();
        CredentialsProvider credsProvider = new BasicCredentialsProvider();
        if (privileged) {
            credsProvider.setCredentials(new AuthScope(null, -1), new UsernamePasswordCredentials(username, password));
        }

        System.out.println("Token result is " + (username.isEmpty() && !password.isEmpty()));
        Collection<Header> headers = new ArrayList<Header>();
        if (username.isEmpty() && !password.isEmpty()) {
            headers.add(new BasicHeader("Authorization", "token " + password));
            System.out.println("Setting authoriation token");
        }
        client = HttpClients.custom().setConnectionManager(connManager)
                                     .setDefaultRequestConfig(requestConfig)
                                     .setDefaultCredentialsProvider(credsProvider)
                                     .setDefaultHeaders(headers)
                                     .build();
        context = HttpClientContext.create();
        context.setAuthCache(new SingleAuthCache(new BasicScheme()));
    }

    @Override
    public boolean privileged() {
        return privileged;
    }

    @Override
    public String doGet(String url) {
        LOG.info(url);
        try {
            final HttpGet httpget = new HttpGet(url);
            httpget.setHeader("Accept", "application/json");

            final ResponseHandler<String> responseHandler = new BasicResponseHandler();
            return client.execute(httpget, responseHandler, context);
        }
        catch (HttpResponseException e) {
            if (e.getStatusCode() == 404) {
                LOG.info(url + " - 404 Not Found", e);
                return "";
            }
            LOG.error(url, e);
        }
        catch (Exception e) {
            LOG.error(url, e);
        }
        return "";
    }

    @Override
    public void doPost(String url) {
        LOG.info(url);
        try {
            client.execute(new HttpPost(url), new ConsumingResponseHandler(), context);
        }
        catch (Exception e) {
            LOG.error(url, e);
        }
    }

    @Override
    public void doPut(String url, String content) {
        LOG.info(url);
        try {
            final HttpPut put = new HttpPut(url);
            put.setEntity(new StringEntity(content));
            
            client.execute(put, new ConsumingResponseHandler(), context);
        }
        catch (Exception e) {
            LOG.error(url, e);
        }
    }

    @Override
    public void shutdown() {
        try {
            client.close();
        }
        catch (Exception e) {
            LOG.error("error shutting down", e);
        }
    }
    
    public static final class SingleAuthCache implements AuthCache {
        private AuthScheme authScheme;
        public SingleAuthCache(AuthScheme authScheme) { this.authScheme = authScheme; }
        @Override public void put(HttpHost host, AuthScheme authScheme) { this.authScheme = authScheme; } 
        @Override public AuthScheme get(HttpHost host) { return this.authScheme; } 
        @Override public void remove(HttpHost host) { return; } 
        @Override public void clear() { return; }
    }
    
    public static final class ConsumingResponseHandler implements ResponseHandler<String> {
        @Override
        public String handleResponse(HttpResponse response) {
            final HttpEntity entity = response.getEntity();
            final int statusCode = response.getStatusLine().getStatusCode();
            try {
                if (statusCode >= 300 && statusCode != 302) {
                    LOG.error("Failed to PUT/POST\n" + EntityUtils.toString(entity));
                }
                EntityUtils.consume(entity);
            } catch (IOException e) {
                LOG.error("Failed to consume response entity");
            }
            return "";
        }
    }

    private static Registry<ConnectionSocketFactory> getSocketFactoryRegistry() {
        try {
            SSLContextBuilder builder = SSLContexts.custom();
            builder.loadTrustMaterial(null, new TrustStrategy() {
                @Override
                public boolean isTrusted(X509Certificate[] chain, String authType) throws CertificateException {
                    return true;
                }
            });

            SSLContext sslContext = builder.build();
            SSLConnectionSocketFactory sslsf =
                new SSLConnectionSocketFactory(sslContext, SSLConnectionSocketFactory.ALLOW_ALL_HOSTNAME_VERIFIER);

            return RegistryBuilder
                .<ConnectionSocketFactory>create().register("https", sslsf)
                .build();
        } catch (NoSuchAlgorithmException e) {
            throw new RuntimeException(e);
        } catch (KeyStoreException e) {
            throw new RuntimeException(e);
        } catch (KeyManagementException e) {
            throw new RuntimeException(e);
        }
    }
}
