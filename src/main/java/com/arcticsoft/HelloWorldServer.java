package com.arcticsoft;

import io.undertow.Undertow;
import io.undertow.server.handlers.PathHandler;
import io.undertow.server.session.*;
import io.undertow.util.Headers;
import io.undertow.util.StatusCodes;

import java.util.Deque;
import java.util.Map;

public class HelloWorldServer {

    public static void main(String[] args) {
        PathHandler pathHandler = new PathHandler();
        pathHandler.addPrefixPath("/", exchange -> {
            StringBuilder sb = new StringBuilder();
            sb.append("<form action='addToSession' >");
            sb.append("<label>Attribute Name</label>");
            sb.append("<input name='attrName' />");
            sb.append("<label>Attribute Value</label>");
            sb.append("<input name='value' />");
            sb.append("<button>Save to Session</button>");
            // To retrive the SessionManager use the attachmentKey
            SessionManager sm = exchange.getAttachment(SessionManager.ATTACHMENT_KEY);
            // same goes to SessionConfig
            SessionConfig sessionConfig = exchange.getAttachment(SessionConfig.ATTACHMENT_KEY);
            sb.append("</form>");
            sb.append("<a href='/destroySession'>Destroy Session</a>");
            sb.append("<br/>");

            Session session = sm.getSession(exchange, sessionConfig);
            if (session == null) session = sm.createSession(exchange, sessionConfig);

            sb.append("<ul>");
            for (String string : session.getAttributeNames()) {
                sb.append("<li>").append(string).append(" : ").append(session.getAttribute(string)).append("</li>");
            }
            sb.append("</ul>");

            exchange.getResponseHeaders().add(Headers.CONTENT_TYPE, "text/html;");
            exchange.getResponseSender().send(sb.toString());
        });
        pathHandler.addPrefixPath("/addToSession", exchange -> {
            SessionManager sm = exchange.getAttachment(SessionManager.ATTACHMENT_KEY);
            SessionConfig sessionConfig = exchange.getAttachment(SessionConfig.ATTACHMENT_KEY);

            Map<String, Deque<String>> reqParams = exchange.getQueryParameters();
            Session session = sm.getSession(exchange, sessionConfig);
            if (session == null) session = sm.createSession(exchange, sessionConfig);

            Deque<String> deque = reqParams.get("attrName");
            Deque<String> dequeVal = reqParams.get("value");
            session.setAttribute(deque.getLast(), dequeVal.getLast());

            exchange.setStatusCode(StatusCodes.TEMPORARY_REDIRECT);
            exchange.getResponseHeaders().put(Headers.LOCATION, "/");
            exchange.getResponseSender().close();
        });
        pathHandler.addPrefixPath("/destroySession", exchange -> {
            SessionManager sm = exchange.getAttachment(SessionManager.ATTACHMENT_KEY);
            SessionConfig sessionConfig = exchange.getAttachment(SessionConfig.ATTACHMENT_KEY);
            Session session = sm.getSession(exchange, sessionConfig);
            if (session == null) session = sm.createSession(exchange, sessionConfig);
            session.invalidate(exchange);

            exchange.setStatusCode(StatusCodes.TEMPORARY_REDIRECT);
            exchange.getResponseHeaders().put(Headers.LOCATION, "/");
            exchange.getResponseSender().close();
        });

        SessionManager sessionManager = new InMemorySessionManager("SESSION_MANAGER");
        SessionCookieConfig sessionConfig = new SessionCookieConfig();
        /*
         * Use the sessionAttachmentHandler to add the sessionManager and
         * sessionCofing to the exchange of every request
         */
        SessionAttachmentHandler sessionAttachmentHandler = new SessionAttachmentHandler(sessionManager, sessionConfig);
        // set as next handler your root handler
        sessionAttachmentHandler.setNext(pathHandler);

        System.out.println("Open the url and fill the form to add attributes into the session");
        Undertow server = Undertow.builder().addHttpListener(8080, "localhost").setHandler(sessionAttachmentHandler).build();
        server.start();
    }
}