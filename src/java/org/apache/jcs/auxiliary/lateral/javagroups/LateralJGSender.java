package org.apache.jcs.auxiliary.lateral.javagroups;

/*
 * Copyright 2002-2004 The Apache Software Foundation.
 *
 * Licensed under the Apache License, Version 2.0 (the "License")
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

import java.io.BufferedReader;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.OutputStream;
import java.io.Reader;
import java.io.Serializable;

import java.util.Vector;
import java.util.Iterator;

import java.net.InetAddress;
import java.net.Socket;

import org.apache.jcs.auxiliary.lateral.LateralCacheInfo;
import org.apache.jcs.auxiliary.lateral.LateralElementDescriptor;

import org.apache.jcs.auxiliary.lateral.behavior.ILateralCacheAttributes;
import org.apache.jcs.auxiliary.lateral.LateralCacheAttributes;

import org.apache.jcs.auxiliary.lateral.javagroups.utils.JGSocketOpener;
import org.apache.jcs.auxiliary.lateral.javagroups.utils.JGRpcOpener;
import org.apache.jcs.auxiliary.lateral.javagroups.behavior.IJGConstants;

import org.javagroups.JChannel;
import org.javagroups.Channel;
import org.javagroups.Message;
import org.javagroups.blocks.RpcDispatcher;
import org.javagroups.util.RspList;
import org.javagroups.blocks.GroupRequest;

import org.apache.jcs.engine.CacheElement;

import org.apache.jcs.engine.behavior.ICacheElement;

import org.apache.jcs.auxiliary.lateral.javagroups.behavior.ILateralCacheJGListener;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

/**
 * This class is based on the log4j SocketAppender class. I'm using a differnet
 * repair structure, so it is significant;y different.
 *
 * @version $Id$
 */
public class LateralJGSender implements IJGConstants
{
    private final static Log log =
        LogFactory.getLog( LateralJGSender.class );

    private ILateralCacheAttributes ilca;

    private String remoteHost;
    private InetAddress address;
    int port = 1111;

    private Channel javagroups;
    private RpcDispatcher disp;

    //private ObjectOutputStream oos;
    //private Socket socket;
    int counter = 0;

    /**
     * Only block for 5 seconds before timing out on startup.
     */
    private final static int openTimeOut = 5000;


    /**
     * Constructor for the LateralJGSender object
     *
     * @param lca
     * @exception IOException
     */
    public LateralJGSender( ILateralCacheAttributes lca )
        throws IOException
    {
        this.ilca = lca;
        init( lca.getUdpMulticastAddr(), lca.getUdpMulticastPort() );
    }


    /**
     * Description of the Method
     *
     * @param host
     * @param port
     * @exception IOException
     */
    protected void init( String host, int port )
        throws IOException
    {
        this.port = port;
        this.address = getAddressByName( host );
        this.remoteHost = host;

        try
        {
            log.debug( "Attempting connection to " + address.getHostName() );
            //socket = new Socket( address, port );

            JGConnectionHolder holder = JGConnectionHolder.getInstance(ilca);
            javagroups = holder.getChannel();
            disp = holder.getDispatcher();

            if ( javagroups == null )
            {
                throw new IOException( "javagroups is null" );
            }

        }
        catch ( java.net.ConnectException e )
        {
            log.debug( "Remote host " + address.getHostName() + " refused connection." );
            throw e;
        }
        catch ( Exception e )
        {
            log.debug( "Could not connect to " + address.getHostName() +
                ". Exception is " + e );
            throw new IOException(e.getMessage());
        }

    }
    // end constructor

    /**
     * Gets the addressByName attribute of the LateralJGSender object
     *
     * @return The addressByName value
     * @param host
     */
    private InetAddress getAddressByName( String host )
    {
        try
        {
            return InetAddress.getByName( host );
        }
        catch ( Exception e )
        {
            log.error( "Could not find address of [" + host + "]", e );
            return null;
        }
    }


    /**
     * Sends commands to the lateral cache listener.
     *
     * @param led
     * @exception IOException
     */
    public void send( LateralElementDescriptor led )
        throws IOException
    {
        log.debug( "sending LateralElementDescriptor" );

        if ( led == null )
        {
            return;
        }

        if ( address == null )
        {
            throw new IOException( "No remote host is set for LateralJGSender." );
            //return;
        }

//        if ( oos != null )
//        {
        try
        {

            Message send_msg = new Message( null, null, led );

            javagroups.send( send_msg );

//                oos.writeObject( led );
//                oos.flush();
//                if ( ++counter >= RESET_FREQUENCY )
//                {
//                    counter = 0;
//                    // Failing to reset the object output stream every now and
//                    // then creates a serious memory leak.
//                    log.info( "Doing oos.reset()" );
//                    oos.reset();
//                }
//            }
//            catch ( IOException e )
//            {
//                //oos = null;
//                log.error( "Detected problem with connection: " + e );
//                throw e;
        }
        catch ( Exception e )
        {
            log.error( "Detected problem with connection: " + e );
            throw new IOException( e.getMessage() );
        }
//        }
    }


    /**
     * Sends commands to the lateral cache listener and gets a response. I'm
     * afraid that we could get into a pretty bad blocking situation here. This
     * needs work. I just wanted to get some form of get working. Will need some
     * sort of timeout.
     *
     * @return
     * @param led
     * @exception IOException
     */
    public ICacheElement sendAndReceive( LateralElementDescriptor led )
        throws IOException
    {
        ICacheElement ice = null;

        log.debug( "sendAndReceive led" );

        if ( led == null )
        {
            return null;
        }

        if ( address == null )
        {
            throw new IOException( "No remote host is set for LateralJGSender." );
            //return;
        }

//        if ( oos != null )
//        {
        try
        {

            try
            {

                RspList rsp_list = disp.callRemoteMethods( null, "handleGet", (String)led.ce.getCacheName(), (Serializable)led.ce.getKey(),
                    GroupRequest.GET_ALL, 1000 );

                log.debug( "rsp_list = " + rsp_list );
                Vector vec = rsp_list.getResults();
                log.debug( "rsp_list size = " + vec.size() );
                Iterator it = vec.iterator();

                while ( it.hasNext() )
                {
                    ice = ( ICacheElement ) it.next();
                    if ( ice != null )
                    {
                        break;
                    }
                }

            }
            catch ( Exception e )
            {
                log.error( e );
            }

        }
        catch ( Exception e )
        {
            log.error( "Detected problem with connection: " + e );
            throw new IOException( e.getMessage() );
        }
//        }
        return ice;
    }// end sendAndReceive

    // Service Methods //
    /**
     * Description of the Method
     *
     * @param item
     * @param requesterId
     * @exception IOException
     */
    public void update( ICacheElement item, byte requesterId )
        throws IOException
    {
        LateralElementDescriptor led = new LateralElementDescriptor( item );
        led.requesterId = requesterId;
        led.command = led.UPDATE;
        send( led );
    }


    /**
     * Description of the Method
     *
     * @param cacheName
     * @param key
     * @exception IOException
     */
    public void remove( String cacheName, Serializable key )
        throws IOException
    {
        remove( cacheName, key, LateralCacheInfo.listenerId );
    }


    /**
     * Description of the Method
     *
     * @param cacheName
     * @param key
     * @param requesterId
     * @exception IOException
     */
    public void remove( String cacheName, Serializable key, byte requesterId )
        throws IOException
    {
        CacheElement ce = new CacheElement( cacheName, key, null );
        LateralElementDescriptor led = new LateralElementDescriptor( ce );
        led.requesterId = requesterId;
        led.command = led.REMOVE;
        send( led );
    }


    /**
     * Description of the Method
     *
     * @exception IOException
     */
    public void release()
        throws IOException
    {
        // nothing needs to be done
    }


    /**
     * Closes connection used by all LateralJGSenders for this lateral
     * conneciton. Dispose request should come into the facade and be sent to
     * all lateral cache sevices. The lateral cache service will then call this
     * method.
     *
     * @param cache
     * @exception IOException
     */
    public void dispose( String cache )
        throws IOException
    {
        // WILL CLOSE CONNECTION USED BY ALL
        //oos.close();
        //javagroups.
    }


    /**
     * Description of the Method
     *
     * @param cacheName
     * @exception IOException
     */
    public void removeAll( String cacheName )
        throws IOException
    {
        removeAll( cacheName, LateralCacheInfo.listenerId );
    }


    /**
     * Description of the Method
     *
     * @param cacheName
     * @param requesterId
     * @exception IOException
     */
    public void removeAll( String cacheName, byte requesterId )
        throws IOException
    {
        CacheElement ce = new CacheElement( cacheName, "ALL", null );
        LateralElementDescriptor led = new LateralElementDescriptor( ce );
        led.requesterId = requesterId;
        led.command = led.REMOVEALL;
        send( led );
    }


    /**
     * Description of the Method
     *
     * @param args
     */
    public static void main( String args[] )
    {
        try
        {
            LateralJGSender lur = null;
            LateralCacheAttributes lca = new LateralCacheAttributes();
            lca.setHttpServer( "localhost:8181" );
            lur = new LateralJGSender( lca );

            // process user input till done
            boolean notDone = true;
            String message = null;
            // wait to dispose
            BufferedReader br = new BufferedReader( new InputStreamReader( System.in ) );

            while ( notDone )
            {
                System.out.println( "enter mesage:" );
                message = br.readLine();
                CacheElement ce = new CacheElement( "test", "test", message );
                LateralElementDescriptor led = new LateralElementDescriptor( ce );
                lur.send( led );
            }
        }
        catch ( Exception e )
        {
            System.out.println( e.toString() );
        }
    }

}
// end class