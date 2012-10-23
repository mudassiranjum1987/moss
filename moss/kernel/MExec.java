/*
 *	MExec.java -- execution engine for MOSS
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

package moss.kernel;

import moss.fs.*;
import moss.user.*;

import java.util.*;

/**
 * the MExec class provides the necessary to allow the kernel to
 * load some program from the file-system
 */

public class MExec
{
	//{{{  public stuff
	/** invalid executable */
	public static final int EXEC_INVALID = 0;
	/** Java class (that should implement an appropriate executable interface) */
	public static final int EXEC_CLASS = 1;
	/** script-based command */
	public static final int EXEC_SCRIPT = 2;

	//}}}
	//{{{  private stuff
	private int type;		/* one of the EXEC_ constants above */
	private int errorcode;		/* error-code if something goes wrong */
	private String hashbang[];	/* array of arguments if a hash-bang script */
	private MExecCL cloader;	/* class loader for Java classes */

	private int exec_refcount;	/* reference count for the exec */
	//}}}


	//{{{  private class-loader
	/**
	 * the MExecCL class implements the ClassLoader for MOSS, reading
	 * things out of the filesystem.
	 */
	private static class MExecCL extends ClassLoader
	{
		//{{{  private attributes
		/** actual class name (when discovered) */
		String classname;
		/** actual class (when discovered) */
		Class eclass;
		/** byte-array that contains the class */
		byte classdata[];
		//}}}
		
		//{{{  public MExecCL (String path, byte data[])
		/**
		 * constructor for this loader
		 *
		 * @param path path to the original executable (for hash-code)
		 * @param data the bytes that make up this class
		 */
		public MExecCL (String path, byte data[])
		{
			int hc;
			String extra = "";

			eclass = null;
			classdata = data;

			hc = path.hashCode();
			if (hc < 0) {
				hc = -(hc + 1);
				extra = "N";
			}
			classname = "MExec" + extra + hc;
		}
		//}}}

		//{{{  public Class findClass (String name) throws ClassNotFoundException
		/**
		 * called to find a loadable user-class (following the delegation model).
		 * Inside MOSS, this expects to be called within the context of a process
		 *
		 * @param name name of the class to load
		 */
		public Class findClass (String name) throws ClassNotFoundException
		{
			/*
			 *	note: this is a bit messy because of the way BlueJ changes
			 *	things -- the loader might be called for a range of related
			 *	classes that are actually built-in MOSS classes
			 */

			// MKernel.log_msg ("MExec:MExecCL::findClass(): name = [" + name + "], classname = [" + ((classname == null) ? "(null)" : classname) + "]" +
			// 		", eclass = [" + ((eclass == null) ? "(null)" : eclass) + "]");
			
			/* if this is called with eclass set, but "classname" doesn't match "name", look for "name" in /lib/ */
			if (eclass != null) {
				if (!name.equals (classname)) {
					/* not this class itself.. */
					byte cbuf[];
					Class cc;

					cbuf = MExec.readfile ("/lib/" + name, 0, null);
					if (cbuf == null) {
						// MKernel.log_msg ("MExec:MExecCL::findFlass(): name = [" + name + "], didn\'t find in /lib, trying builtin...");
						try {
							cc = Class.forName (name);
							return cc;
						} catch (NoClassDefFoundError e) {
							throw new ClassNotFoundException (name);
						} catch (ClassFormatError e) {
							throw new ClassNotFoundException (name);
						} catch (Exception e) {
							throw new ClassNotFoundException (name);
						}
					}
					try {
						cc = defineClass (name, cbuf, 0, cbuf.length);
					} catch (NoClassDefFoundError e) {
						throw new ClassNotFoundException (name);
					} catch (ClassFormatError e) {
						throw new ClassNotFoundException (name);
					} catch (Exception e) {
						throw new ClassNotFoundException (name);
					}

					return cc;
				} else {
					return eclass;
				}
			}

			try {
				/* if the name given is sensible, use it */
				if ((name.length() > 5) && !name.substring(0,5).equals ("MExec")) {
					/* try with this name */
					try {
						eclass = defineClass (name, classdata, 0, classdata.length);
					} catch (NoClassDefFoundError e) {
						/* probably not this class -- try loading it using the default class-loader */
						Class cc;

						cc = Class.forName (name);
						return cc;
					}
				} else {
					eclass = defineClass (null, classdata, 0, classdata.length);
				}
			} catch (NoClassDefFoundError e) {
				// MKernel.log_msg ("MExec:MExecCL::findFlass(): name = [" + name + "], got NoClassDefFoundError: " + e.getMessage());
				throw new ClassNotFoundException (name);
			} catch (ClassFormatError e) {
				throw new ClassNotFoundException (name);
			} catch (Exception e) {
				throw new ClassNotFoundException (name);
			}

			classname = eclass.getName ();

			// MKernel.log_msg ("MExec:MExecCL::findClass(): after defining, classname = [" + classname + "]");

			return eclass;
		}
		//}}}
		//{{{  public String getclassname ()
		/**
		 * returns the fudged class-name for this loader (based on the path given earlier)
		 *
		 * @return a string representing the class name (as a Java-compatible class name)
		 */
		public String getclassname ()
		{
			return classname;
		}
		//}}}
	}
	//}}}

	//{{{  public MExec ()
	/**
	 * constructor: creates a new MExec
	 */
	public MExec ()
	{
		errorcode = 0;
		type = EXEC_INVALID;
		hashbang = null;
		cloader = null;
		exec_refcount = 0;

		return;
	}
	//}}}
	//{{{  private byte[] readfile (String path, int maxread)
	/**
	 * reads the contents of a MOSS file-system object
	 *
	 * @param path file to read
	 * @param maxread maximum number of bytes to read, or 0 for all of it
	 *
	 * @return array of bytes from the file, or null on error
	 */
	private static byte[] readfile (String path, int maxread, MExec mxc)
	{
		byte rbuf[];
		MFile fh;
		int r, in, i;

		fh = new MFile ();
		fh.refcount = 1;
		fh.pdata = null;
		fh.offset = 0;
		fh.fileif = null;
		fh.dirif = null;

		/* remove any trailing slash from the path, although not expecting one particularly.. */
		if ((path.length() > 1) && (path.charAt (path.length() - 1) == '/')) {
			r = MFileSystem.open (path.substring (0, path.length() - 1), fh, MFileOps.OPEN_READ, 0400);
		} else {
			r = MFileSystem.open (path, fh, MFileOps.OPEN_READ, 0400);
		}

		if (r < 0) {
			if (mxc != null) {
				mxc.errorcode = r;
			}
			return null;
		}

		in = 0;
		rbuf = null;
		while ((in < maxread) || (maxread == 0)) {
			int toread = (maxread > 0) ? (maxread - in) : 8192;
			byte tmpbuf[] = new byte[toread];

			i = fh.fileif.read (fh, tmpbuf, toread);
			if (i < 0) {
				/* errored, close file */
				if (mxc != null) {
					mxc.errorcode = i;
				}
				fh.fileif.close (fh);
				fh = null;
				return null;
			} else if (i > 0) {
				/* got some stuff, add it to rbuf */
				if (rbuf == null) {
					rbuf = new byte[i];

					System.arraycopy (tmpbuf, 0, rbuf, 0, i);
				} else {
					byte xbuf[] = new byte[rbuf.length + i];

					System.arraycopy (rbuf, 0, xbuf, 0, rbuf.length);
					System.arraycopy (tmpbuf, 0, xbuf, rbuf.length, i);
					rbuf = xbuf;
				}
				in += i;
			} else {
				/* end-of-file */
				break;
			}
		}

		if (rbuf == null) {
			rbuf = new byte[0];
		}

		fh.fileif.close (fh);
		fh = null;

		return rbuf;
	}
	//}}}
	//{{{  public int setpath (String path)
	/**
	 * sets the file for an MExec -- this will look at the file and attempt
	 * to determine what type it is.
	 *
	 * @param path path to executable file
	 * 
	 * @return 0 on success, or &lt; 0 indicating error
	 */
	public int setpath (String path)
	{
		byte rbuf[];

		/* read the first 128 bytes (or less) from the file */
		rbuf = readfile (path, 128, this);
		if (rbuf == null) {
			errorcode = -MSystem.ENOENT;
			return errorcode;
		}
		if (rbuf.length < 4) {
			errorcode = -MSystem.ENOEXEC;
			return errorcode;
		}

		/* try and guess what it is ... */
		if ((rbuf[0] == (byte)0xca) && (rbuf[1] == (byte)0xfe) && (rbuf[2] == (byte)0xba) && (rbuf[3] == (byte)0xbe)) {
			/* most probably a Java .class file  -- read the rest of it */
			rbuf = readfile (path, 0, this);

			/* got the whole lot in "rbuf" now -- build a ClassLoader that will load it */
			cloader = new MExecCL (path, rbuf);
			type = EXEC_CLASS;
		} else if ((rbuf[0] == (byte)'#') && (rbuf[1] == (byte)'!')) {
			/* some sort of shebang (hash-bang) */
			int idx;
			byte firstline[];
			String fline;

			/* find end-of-line */
			for (idx=2; idx<rbuf.length; idx++) {
				if (rbuf[idx] == (byte)'\n') {
					break;
				}
			}
			if (idx == rbuf.length) {
				/* ran out */
				errorcode = -MSystem.ENOEXEC;
				return errorcode;
			}
			firstline = new byte[idx - 2];
			System.arraycopy (rbuf, 2, firstline, 0, idx - 2);
			fline = new String (firstline);
			type = EXEC_SCRIPT;
			hashbang = MStdLib.split_string (fline + " " + path);
		} else {
			type = EXEC_INVALID;
		}

		return 0;
	}
	//}}}

	//{{{  public synchronized ClassLoader getclassloader (String name)
	/**
	 * returns a class loader for some executable
	 *
	 * @return class-loader on success or null on error
	 */
	public synchronized ClassLoader getclassloader ()
	{
		return (ClassLoader)cloader;
	}
	//}}}
	//{{{  public String classnameof (ClassLoader mcloader)
	/**
	 * returns a string providing a class-name for MExecCL objects.  This is needed
	 * since the Java class-loading mechanism (sensibly) won't allow things like lowercase
	 * file-names as class names.  This just fudges a name -- the actual classloader
	 * knows what it really is.
	 */
	public String classnameof ()
	{
		return cloader.classname;
	}
	//}}}
	//{{{  public String[] shebangof ()
	/**
	 * returns the array of strings that represents the hash-bang path, with this executable's name attached
	 */
	public String[] shebangof ()
	{
		return hashbang;
	}
	//}}}
	//{{{  public int errorof ()
	/**
	 * returns the last reported error for this MExec object
	 *
	 * @return error-code
	 */
	public int errorof ()
	{
		return errorcode;
	}
	//}}}
	//{{{  public int typeof ()
	/**
	 * return the MExec type of this object
	 *
	 * @return EXEC_... type constant
	 */
	public int typeof ()
	{
		return type;
	}
	//}}}


	/*
	 *	note: this stuff below is _UGLY_..  Basically it seems the class can't be
	 *	re-loaded if it already is since class-loaders "partition" the JVM.  Attempting
	 *	to load a second one causes problems with inner-classes later on.  The code below
	 *	stops that happening, but currently doesn't deal with "trash_active()", so once
	 *	loaded, executables are never removed..  (not a serious problem at the moment)
	 */


	//{{{  private static stuff
	private static Hashtable loaded = new Hashtable ();

	//}}}
	//{{{  public static MExec load_active (String path)
	/**
	 * this is called to return an already loaded MExec for a path
	 * (ie something that is already running).
	 *
	 * @param path path to executable
	 * 
	 * @return MExec class or null if not found
	 */
	public static MExec load_active (String path)
	{
		MExec exec = null;

		synchronized (loaded) {
			exec = (MExec)loaded.get (path);
			if (exec != null) {
				exec.exec_refcount++;
			}
		}
		return exec;
	}
	//}}}
	//{{{  public static void store_active (MExec exec, String path)
	/**
	 * adds an MExec to the list of those loaded
	 *
	 * @param exec MExec representing the executable
	 * @param path path to the executable
	 */
	public static void store_active (MExec exec, String path)
	{
		synchronized (loaded) {
			exec.exec_refcount = 1;
			loaded.put (path, exec);
		}
		return;
	}
	//}}}
	//{{{  public static void trash_active (MExec exec, String path)
	/**
	 * this removes a loaded MExec, called when a process is done using it
	 * (during process shutdown)
	 *
	 * @param exec MExec representing the executable
	 * @param path path to the executable
	 */
	public static void trash_active (MExec exec, String path)
	{
		synchronized (loaded) {
			if (loaded.contains (exec)) {
				exec.exec_refcount--;
				if (exec.exec_refcount == 0) {
					loaded.remove (path);
				}
			}
		}
		return;
	}
	//}}}
}


