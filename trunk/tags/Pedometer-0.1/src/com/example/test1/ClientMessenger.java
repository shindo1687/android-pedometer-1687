package com.example.test1;

import java.util.ArrayList;

import android.os.Message;
import android.os.Messenger;
import android.os.RemoteException;

/**
 * サービスに接続しているクライアントのメッセンジャー管理
 * @author john
 *
 */
public class ClientMessenger {
	private ArrayList<Messenger> mClients = new ArrayList<Messenger>();
	
	public void add(Messenger messenger) {
		mClients.add(messenger);
	}
	
	public void remove(Messenger messenger) {
		mClients.remove(messenger);
	}
	
	public void sendMessage(Message msg) {
    	for (int i = mClients.size() - 1; i >= 0; i--) {
            try {
                mClients.get(i).send(msg);
            } catch (RemoteException e) {
                // The client is dead.  Remove it from the list;
                // we are going through the list from back to front
                // so this is safe to do inside the loop.
                mClients.remove(i);
            }
        }
    }
}
