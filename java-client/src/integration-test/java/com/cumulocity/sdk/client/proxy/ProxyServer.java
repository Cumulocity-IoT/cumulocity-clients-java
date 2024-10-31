package com.cumulocity.sdk.client.proxy;

import lombok.SneakyThrows;
import org.eclipse.jetty.client.api.Request;
import org.eclipse.jetty.proxy.ConnectHandler;
import org.eclipse.jetty.proxy.ProxyServlet;
import org.eclipse.jetty.server.Server;
import org.eclipse.jetty.server.ServerConnector;
import org.eclipse.jetty.server.handler.HandlerCollection;
import org.eclipse.jetty.servlet.ServletContextHandler;
import org.eclipse.jetty.servlet.ServletHolder;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.util.Base64;

import static org.apache.commons.lang3.StringUtils.isNotBlank;

public class ProxyServer {
    private String basicAuthUsername;
    private String basicAuthPassword;

    private Server server;

    private void initialize() {
        server = new Server();
        ServerConnector connector = new ServerConnector(server);
        connector.setPort(0);
        server.addConnector(connector);

        HandlerCollection handlers = new HandlerCollection();
        server.setHandler(handlers);

        ServletContextHandler context = new ServletContextHandler(handlers, "/", ServletContextHandler.SESSIONS);
        ServletHolder proxyServlet = new ServletHolder(new AuthenticatedProxyServlet());
        context.addServlet(proxyServlet, "/*");

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

    private boolean handleAuthentication(HttpServletRequest request) {
        if (isProxyBasicAuthorization()) {
            String basicHeaderValue = "Basic " + Base64.getEncoder().encodeToString((basicAuthUsername + ":" + basicAuthPassword).getBytes());
            return basicHeaderValue.equals(request.getHeader("Proxy-Authorization"));
        }
        return true;
    }

    private class AuthenticatedProxyServlet extends ProxyServlet {
        @SneakyThrows
        @Override
        protected void sendProxyRequest(HttpServletRequest clientRequest, HttpServletResponse proxyResponse, Request proxyRequest) {
            if (!handleAuthentication(clientRequest)) {
                proxyResponse.setHeader("Proxy-Authenticate", "Basic realm=\"proxy\"");
                proxyResponse.sendError(407);
                return;
            }
            super.sendProxyRequest(clientRequest, proxyResponse, proxyRequest);
        }
    }

    private class AuthenticatedConnectHandler extends ConnectHandler {
        @Override
        protected boolean handleAuthentication(HttpServletRequest request, HttpServletResponse response, String address) {
            if (!ProxyServer.this.handleAuthentication(request)) {
                response.setHeader("Proxy-Authenticate", "Basic realm=\"proxy\"");
                return false;
            }
            return true;
        }
    }
}
