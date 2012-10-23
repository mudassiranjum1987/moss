/*
 *	MSocketAddr.java -- socket address for MOSS
 *	Copyright (C) 2005 Fred Barnes <frmb@kent.ac.uk>
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
import moss.user.*;

import java.util.*;
import java.io.*;
import java.net.*;


/**
 * this class provides an abstraction of a socket address for MOSS.  It
 * incorporates both the address and the port (like the POSIX's sockaddr_in),
 * but also the family for handling addresses other than AF_INET
 */

public class MSocketAddr
{
	//{{{  address family constants
	/** internet address */
	public static final int AF_INET = 0;
	/** unix (MOSS) address */
	public static final int AF_UNIX = 1;

	//}}}
	//{{{  contents of the address
	/** address family */
	public int family;
	/** address as bytes */
	public byte address[];
	/** port */
	public int port;
	/** path for a UNIX socket */
	public String path;
	//}}}


	//{{{  public MSocketAddr ()
	/**
	 * initialises the address to some sensible defaults
	 */
	public MSocketAddr ()
	{
		int i;

		family = AF_INET;
		address = new byte[4];
		for (i=0; i<4; i++) {
			address[i] = 0x00;
		}
		port = 0;
		path = null;
	}
	//}}}
	//{{{  public MSocketAddr (String path)
	/**
	 * initialises the address with a UNIX socket path
	 *
	 * @param path path to the socket (in MOSS-land)
	 */
	public MSocketAddr (String path)
	{
		family = AF_UNIX;
		address = null;
		port = -1;
		this.path = path;
	}
	//}}}
}

