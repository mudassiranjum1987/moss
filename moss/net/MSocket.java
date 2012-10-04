/*
 *	MSocket.java -- socket provision for MOSS
 *	Copyright (C) 2004 Fred Barnes <frmb@kent.ac.uk>
 *
 *	This program is free software; you can redistribute it and/or modify
 *	it under the terms of the GNU General Public License as published by
 *	the Free Software Foundation; either version 2 of the License, or
 *	(at your option) any later version.
 *
 *	This program is distributed in the hope that it will be useful,
 *	but WITHOUT ANY WARRANTY; without even the implied warranty of
 *	MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 *	GNU General Public License for more details.
 *
 *	You should have received a copy of the GNU General Public License
 *	along with this program; if not, write to the Free Software
 *	Foundation, Inc., 675 Mass Ave, Cambridge, MA 02139, USA.
 */

package moss.net;

import moss.kernel.*;
import moss.fs.*;
import moss.user.*;

import java.util.*;
import java.io.*;
import java.net.*;


/**
 * this class provides the various socket mechanisms required of MOSS
 */

public class MSocket
{
	//{{{  domain constants
	/** internet/IP based sockets */
	public static final int PF_INET = 0;
	/** unix domain sockets */
	public static final int PF_UNIX = 1;

	//}}}
	//{{{  socket type constants
	/** stream-based TCP sockets */
	public static final int SOCK_STREAM = 0;
	/** packet-based UDP socket */
	public static final int SOCK_DGRAM = 1;

	//}}}


	//{{{  private class Endpoint implements MFileOps
	/**
	 * the Endpoint class attaches to the user-side of MOSS providing
	 * a socket implementation
	 */
	private static class Endpoint implements MFileOps
	{
		//{{{  public variables
		public int domain;		/** socket domain */
		public int type;		/** socket type */

		public ServerSocket ssock;	/** java server socket */
		public Socket csock;		/** java client socket */

		public MSocketAddr localaddr;	/** local address */
		public MSocketAddr remoteaddr;	/** remote address */
		//}}}


		//{{{  public Endpoint ()
		/**
		 * initialises class vars to default values
		 */
		public Endpoint ()
		{
			domain = -1;
			type = -1;
			ssock = null;
			csock = null;

			return;
		}
		//}}}

	
		//{{{  public int open (MFile handle, int flags)
		/**
		 * called once when the socket is created
		 *
		 * @param handle MFile handle to associate with this
		 * @param flags open flags (not really relevant for sockets)
		 *
		 * @return 0 on success or &lt; 0 indicating error
		 */
		public int open (MFile handle, int flags)
		{
			return -MSystem.ENOSYS;
		}
		//}}}
		//{{{  public int close (MFile handle)
		/**
		 * called once when a socket is closed
		 *
		 * @param handle MFile handle associated with the socket
		 *
		 * @return 0 on success or &lt; 0 indicating error
		 */
		public int close (MFile handle)
		{
			return -MSystem.ENOSYS;
		}
		//}}}
		//{{{  public int lseek (MFile handle, int offset, int whence)
		/**
		 * seek to a specific offset (not relevant for sockets)
		 *
		 * @param handle MFile handle associated with the socket
		 * @param offset offset to seek to
		 * @param whence seek origin
		 *
		 * @return new absolute offset or &lt; 0 indicating error
		 */
		public int lseek (MFile handle, int offset, int whence)
		{
			return -MSystem.ESPIPE;
		}
		//}}}
		//{{{  public int read (MFile handle, byte buffer[], int count)
		/**
		 * reads bytes from the socket
		 *
		 * @param handle MFile handle associated with the socket
		 * @param buffer buffer where data will be stored
		 * @param count maxmimum amount of data to read
		 *
		 * @return number of bytes read on success, 0 on end-of-file, or &lt; 0 indicating error
		 */
		public int read (MFile handle, byte buffer[], int count)
		{
			return -MSystem.ENOSYS;
		}
		//}}}
		//{{{  public int write (MFile handle, byte buffer[], int count)
		/**
		 * writes bytes to the socket
		 *
		 * @param handle MFile handle associated with the socket
		 * @param buffer buffer to transmit
		 * @param count maxmimum amount of data to write
		 *
		 * @return number of bytes written (or queued for write) on success, or &lt; 0 indicating error
		 */
		public int write (MFile handle, byte buffer[], int count)
		{
			return -MSystem.ENOSYS;
		}
		//}}}
		//{{{  public int fcntl (MFile handle, int op, int arg)
		public int fcntl (MFile handle, int op, int arg)
		{
			return -MSystem.EIO;
		}
		//}}}
	}
	//}}}

	//{{{  public static int make_endpoint (MFile handle, int domain, int type)
	/**
	 * creates a new socket endpoint.  This does very little until the socket
	 * is actually actioned in some way (e.g. listen/connect/receive)
	 *
	 * @param handle file-handle that will be associated with the socket endpoint
	 * @param domain domain of this socket (PF_...)
	 * @param type type of this socket (SOCK_...)
	 *
	 * @return 0 on success, or &lt; 0 indicating error
	 */
	public static int make_endpoint (MFile handle, int domain, int type)
	{
		Endpoint endp = new Endpoint ();

		switch (domain) {
		case PF_INET:
		case PF_UNIX:
			endp.domain = domain;
			break;
		default:
			return -MSystem.EINVAL;
		}
		switch (type) {
		case SOCK_STREAM:
		case SOCK_DGRAM:
			endp.type = type;
			break;
		default:
			return -MSystem.EINVAL;
		}

		handle.fileif = (MFileOps)endp;			/* attach */
		return 0;
	}
	//}}}
}


