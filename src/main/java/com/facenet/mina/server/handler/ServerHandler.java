package com.facenet.mina.server.handler;

import com.facenet.mina.Entity.Login;
import com.facenet.mina.Entity.Logout;
import com.facenet.mina.Entity.Message;
import com.facenet.mina.Entity.Room;
import org.apache.mina.core.service.IoHandlerAdapter;
import org.apache.mina.core.session.IdleStatus;
import org.apache.mina.core.session.IoSession;
import org.w3c.dom.Document;
import org.xml.sax.InputSource;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import java.io.StringReader;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

public class ServerHandler extends IoHandlerAdapter {
    private final Map<Long, String> user = new ConcurrentHashMap<>();

    private Room room = new Room();

    private List<IoSession> ioSessionList = new ArrayList<>();

    /**
     *
     * @param session
     * @param cause
     * @throws Exception
     */
    @Override
    public void exceptionCaught(IoSession session, Throwable cause) throws Exception {
        cause.printStackTrace();
    }

    /**
     * return response for client
     * @param session
     * @param message
     */
    private void responseMessage(IoSession session, Message message) {
        session.write(message);
    }

    /**
     *
     * @param session
     * @param message
     * @throws Exception
     */
    @Override
    public void messageReceived(IoSession session, Object message) throws Exception {
        if (message instanceof Login) {
            String username = ((Login) message).getUsername();
            if (!user.containsKey(session.getId())) {
                user.put(session.getId(), username);
                Message newMsg = new Message(username + " join chat room",
                        "Server");
                room.addNewMsg(newMsg);
                ioSessionList.add(session);
                ioSessionList.forEach(session1 -> {
                    if (!session1.equals(session)) {
                        responseMessage(session1, newMsg);
                    } else {
                        session1.write(room);
                    }
                });
            }
        } else if (message instanceof Message) {
            room.addNewMsg((Message) message);
            ioSessionList.forEach(session1 -> {
                responseMessage(session1, (Message) message);
            });
        } else if (message instanceof Logout) {
            String username = user.get(session.getId());
            Message newMsg = new Message(username + " out chat room", "Server");
            room.addNewMsg(newMsg);
            ioSessionList.forEach(session1 -> {
                responseMessage(session1, newMsg);
            });
            session.closeOnFlush();
        } else {
            session.write("Error");
        }
    }

    @Override
    public void sessionIdle(IoSession session, IdleStatus status) throws Exception {
        System.out.println("IDLE " + session.getIdleCount(status));
    }

    @Override
    public void sessionCreated(IoSession session) throws Exception {
        System.out.println(session.getRemoteAddress());
    }
}