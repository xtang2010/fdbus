/*
 * Copyright (C) 2015   Jeremy Chen jeremy_cz@yahoo.com
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

import ipc.fdbus.FdbusClient;
import ipc.fdbus.FdbusClientListener;
import ipc.fdbus.SubscribeItem;
import ipc.fdbus.Fdbus;
import ipc.fdbus.FdbusMessage;
import ipc.fdbus.NFdbExample;
import java.util.ArrayList;
import java.util.Timer;
import java.util.TimerTask;

public class FdbusTestClient 
{
    private class myTestClient extends TimerTask implements FdbusClientListener
    {
        public final static int REQ_METADATA = 0;
        public final static int REQ_RAWDATA = 1;
        public final static int REQ_CREATE_MEDIAPLAYER = 2;
        public final static int NTF_ELAPSE_TIME = 3;
        public final static int NTF_MEDIAPLAYER_CREATED = 4;
        public final static int NTF_MANUAL_UPDATE = 5;

        int mSongId;
        
        private FdbusClient mClient;
        
        public myTestClient(String name)
        {
            mClient = new FdbusClient(name);
            mSongId = 0;
        }

        public FdbusClient client()
        {
            return mClient;
        }
        
        public void onOnline(int sid)
        {
            System.out.println(mClient.endpointName() + ": onOnline is received.");
            ArrayList<SubscribeItem> subscribe_items = new ArrayList<SubscribeItem>();
            subscribe_items.add(new SubscribeItem(NTF_ELAPSE_TIME, "my_filter"));
            subscribe_items.add(new SubscribeItem(NTF_ELAPSE_TIME, "raw_buffer"));
            mClient.subscribe(subscribe_items);
        }
        public void onOffline(int sid)
        {
            System.out.println(mClient.endpointName() + ": onOffline is received.");
        }
        
        public void onReply(FdbusMessage msg)
        {
            if (msg.returnValue() != FdbusMessage.FDB_ST_OK)
            {
                System.out.println(mClient.endpointName() + 
                        ": Error is received at onReply. Error code: " + msg.returnValue());
                return;
            }

            switch (msg.code())
            {
                case REQ_METADATA:
                {
                    try {
                        NFdbExample.NowPlayingDetails np =
                                NFdbExample.NowPlayingDetails.parseFrom(msg.byteArray());
                        System.out.println(mClient.endpointName() + ": now playing info: " + np.toString());
                    } catch (Exception e) {
                        System.out.println(e);
                    }
                }
                break;
                default:
                break;
            }
        }
        
        public void onBroadcast(FdbusMessage msg)
        {
            switch (msg.code())
            {
                case NTF_ELAPSE_TIME:
                {
                    if (msg.topic().equals("my_filter"))
                    {
                        try {
                            NFdbExample.ElapseTime et =
                                    NFdbExample.ElapseTime.parseFrom(msg.byteArray());
                            System.out.println(mClient.endpointName() + 
                                    ": elapse time is received: " + et.toString());
                        } catch (Exception e) {
                            System.out.println(e);
                        }
                    }
                }
                break;
                default:
                break;
            }
        }

        public void run()
        {
            NFdbExample.SongId.Builder builder = NFdbExample.SongId.newBuilder();
            builder.setId(mSongId);
            NFdbExample.SongId song_id = builder.build();
            mClient.invokeAsync(REQ_METADATA, song_id.toByteArray(), null, 0);
        }
    }

    Timer mTimer;
    public void startServerInvoker(TimerTask tt)
    {
        mTimer = new Timer();
        mTimer.schedule(tt, 500, 500);
    }

    private myTestClient createClient(String name)
    {
        myTestClient clt = new myTestClient(name);
        clt.client().setListener(clt);
        return clt;
    }

    public static void main(String[] args)
    {
        Fdbus fdbus = new Fdbus();
        FdbusTestClient clt = new FdbusTestClient();

        ArrayList<myTestClient> clients = new ArrayList<myTestClient>();
        for (String arg : args)
        {
            myTestClient client = clt.createClient(arg + "_client");
            client.client().connect("svc://" + arg);
            clt.startServerInvoker(client);

            clients.add(client);
        }
        
        try{Thread.sleep(500000);}catch(InterruptedException e){System.out.println(e);}
        //Thread.sleep(5000000);
    }
}