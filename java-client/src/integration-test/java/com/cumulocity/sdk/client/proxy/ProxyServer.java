package com.cumulocity.sdk.client.proxy;

import lombok.SneakyThrows;
import org.eclipse.jetty.client.Request;
import org.eclipse.jetty.server.handler.ConnectHandler;
import org.eclipse.jetty.ee10.proxy.AsyncProxyServlet;
import org.eclipse.jetty.server.Server;
import org.eclipse.jetty.server.ServerConnector;
import org.eclipse.jetty.server.Handler.Sequence;
import org.eclipse.jetty.ee10.servlet.ServletContextHandler;
import org.eclipse.jetty.ee10.servlet.ServletHolder;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.util.Base64;

import static org.apache.commons.lang3.StringUtils.isNotBlank;
import static org.eclipse.jetty.http.HttpHeader.PROXY_AUTHENTICATE;
import static org.eclipse.jetty.http.HttpHeader.PROXY_AUTHORIZATION;


public class ProxyServer {
    private String basicAuthUsername;
    private String basicAuthPassword;

    private Server server;

    private void initialize() {
        server = new Server();
        ServerConnector connector = new ServerConnector(server);
        connector.setPort(0);
        server.addConnector(connector);

        Sequence handlers = new Sequence();
        server.setHandler(handlers);

        ServletContextHandler context = new ServletContextHandler("/", ServletContextHandler.SESSIONS);
        ServletHolder proxyServlet = new ServletHolder(new AuthenticatedProxyServlet());
        context.addServlet(proxyServlet, "/*");
        handlers.addHandler(context);

        handlers.addHandler(new AuthenticatedConnectHandler());
    }

    public int getPort() {
        if (!isRunning()) {
            throw new IllegalStateException("Server is not running");
        }
        return server.getURI().getPort();
    }

    public void setBasicAuthUsername(String basicAuthUsername) {
        this.basicAuthUsername = basicAuthUsername;
    }

    public void setBasicAuthPassword(String basicAuthPassword) {
        this.basicAuthPassword = basicAuthPassword;
    }

    public void start() throws Exception {
        if (!isInitialized()) {
            initialize();
        }
        if (!server.isRunning()) {
            server.start();
        }
    }

    public void stop() throws Exception {
        if (isRunning()) {
            server.stop();
        }
    }

    private boolean isInitialized() {
        return server != null;
    }

    private boolean isRunning() {
        return isInitialized() && server.isRunning();
    }

    private boolean isProxyBasicAuthorization() {
        return isNotBlank(basicAuthUsername) && isNotBlank(basicAuthPassword);
    }

    private boolean handleProxyAuthorization(String proxyAuthorizationHeader) {
        if (isProxyBasicAuthorization()) {
            String basicHeaderValue = "Basic " + Base64.getEncoder().encodeToString((basicAuthUsername + ":" + basicAuthPassword).getBytes());
            return basicHeaderValue.equals(proxyAuthorizationHeader);
        }
        return true;
    }

    private class AuthenticatedProxyServlet extends AsyncProxyServlet {
        @SneakyThrows
        @Override
        protected void sendProxyRequest(HttpServletRequest clientRequest, HttpServletResponse proxyResponse, Request proxyRequest) {
            if (!handleProxyAuthorization(clientRequest.getHeader(PROXY_AUTHORIZATION.asString()))) {
                proxyResponse.setHeader(PROXY_AUTHENTICATE.asString(), "Basic realm=\"proxy\"");
                proxyResponse.sendError(407);
                return;
            }
            super.sendProxyRequest(clientRequest, proxyResponse, proxyRequest);
        }

    }

    private class AuthenticatedConnectHandler extends ConnectHandler {
        @Override
        protected boolean handleAuthentication(org.eclipse.jetty.server.Request request, org.eclipse.jetty.server.Response response, String address) {
            if (!ProxyServer.this.handleProxyAuthorization(request.getHeaders().get(PROXY_AUTHORIZATION))) {
                response.getHeaders().add(PROXY_AUTHENTICATE, "Basic realm=\"proxy\"");
                return false;
            }
            return true;
        }
    }

}
