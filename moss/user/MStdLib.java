/*
 *	MStdLib.java -- a "standard library" for MOSS
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

package moss.user;

import moss.kernel.*;
import moss.net.*;

import java.net.*;

/**
 * This class defines various `standard library' functions/methods for MOSS,
 * intended to be used by user-applications.
 */

public class MStdLib
{
	//{{{  public static String strerror (int ecode)
	/**
	 * returns a String containing a description of the given error-code.
	 * the code given may be negative or positive; negative values are simply inverted.
	 *
	 * @param ecode error-code (MSystem.E...)
	 *
	 * @return a String object containing descriptive text, or the empty string (not null)
	 */
	public static String strerror (int ecode)
	{
		if (ecode < 0) {
			ecode *= -1;
		}
		switch (ecode) {
		case MSystem.ESUCCESS:
			return "success";
		case MSystem.EPERM:
			return "operation not permitted";
		case MSystem.ENOENT:
			return "no such file or directory";
		case MSystem.ESRCH:
			return "no such process";
		case MSystem.EINTR:
			return "interrupted system call";
		case MSystem.EIO:
			return "I/O error";
		case MSystem.ENXIO:
			return "no such device or address";
		case MSystem.E2BIG:
			return "argument list too long";
		case MSystem.ENOEXEC:
			return "executable format error";
		case MSystem.EBADF:
			return "bad file descriptor";
		case MSystem.ECHILD:
			return "no child processes";
		case MSystem.EAGAIN:
			return "try again";
		case MSystem.ENOMEM:
			return "out of memory";
		case MSystem.EACCESS:
			return "permission denied";
		case MSystem.EFAULT:
			return "bad address";
		case MSystem.EBLKDEV:
			return "block device required";
		case MSystem.EBUSY:
			return "device or resource busy";
		case MSystem.EEXISTS:
			return "file already exists";
		case MSystem.EXDEV:
			return "cross-device link";
		case MSystem.ENODEV:
			return "no such device";
		case MSystem.ENOTDIR:
			return "not a directory";
		case MSystem.EISDIR:
			return "is a directory";
		case MSystem.EINVAL:
			return "invalid value";
		case MSystem.ENFILE:
			return "file-table overflow";
		case MSystem.EMFILE:
			return "too many open files";
		case MSystem.ENOTTY:
			return "not a typewriter";
		case MSystem.ETXTBSY:
			return "text file busy";
		case MSystem.EFBIG:
			return "file too large";
		case MSystem.ENOSPC:
			return "no space left on device";
		case MSystem.ESPIPE:
			return "illegal seek";
		case MSystem.EROFS:
			return "read-only file system";
		case MSystem.EMLINK:
			return "too many links";
		case MSystem.EPIPE:
			return "broken pipe";
		case MSystem.ENAMETOOLONG:
			return "file-name too long";
		case MSystem.ENOSYS:
			return "unimplemented system call";
		default:
			return "";
		}
	}
	//}}}
	//{{{  public static String sprintf (String fmt, Object args[])
	/**
	 * generates a formatted string, similar to (s)printf.  The supported types for
	 * the format string are "%s" (for a String argument) and "%d" (for an Integer argument).
	 * Also accepts padding information in the form "%40s" for left-padded and "%-40s" for
	 * right-padded.
	 *
	 * @param fmt format string
	 * @param args array of Object arguments
	 *
	 * @return formatted string on success, or null on failure (failed to match
	 * 		arguments to the format string, or bad format string)
	 */
	public static String sprintf (String fmt, Object args[])
	{
		int fidx = 0;
		int slen, alen;
		int i;
		String outstr = "";

		if (fmt == null) {
			return null;
		}
		slen = fmt.length ();
		if (args != null) {
			alen = args.length;
		} else {
			alen = 0;
		}
		/* process */
		i = 0;
		while (i < slen) {
			int j;

			/* scan forward to format */
			j = fmt.indexOf ('%', i);
			if (j < 0) {
				/* no (more) format arguments, append leftovers */
				outstr += fmt.substring (i, slen);
				i = slen;
			} else {
				/* found a format argument */
				boolean dopad = false;
				int pad = 0;

				if (j > i) {
					/* append leading chunk */
					outstr += fmt.substring (i, j);
					i = j;
				}
				if (fidx == alen) {
					/* not enough arguments */
					return null;
				}
				if (j == (slen - 1)) {
					/* illegal % at end-of-string */
					return null;
				}
				i++;
				try {
					String xstr = "";

					if ((fmt.charAt(i) == '-') || ((fmt.charAt(i) >= '0') && (fmt.charAt(i) <= '9'))) {
						/* got a padding argument */
						for (j=i+1; (fmt.charAt(j) >= '0') && (fmt.charAt(j) <= '9'); j++);
						dopad = true;
						pad = Integer.parseInt (fmt.substring (i, j));
						i = j;
					}
					switch (fmt.charAt(i)) {
					case 'd':
						xstr = ((Integer)args[fidx]).toString ();
						fidx++;
						i++;
						break;
					case 's':
						xstr = (String)args[fidx];
						fidx++;
						i++;
						break;
					default:
						return null;
					}
					if (dopad) {
						String pstr;

						if (pad < 0) {
							/* left-justify */
							int pleft = (-pad) - xstr.length();

							if (pleft > 0) {
								byte spaces[] = new byte[pleft];

								for (int k=0; k<pleft; k++) {
									spaces[k] = ' ';
								}
								pstr = new String (spaces);
							} else {
								pstr = "";
							}
							outstr += (xstr + pstr);
						} else {
							/* right-justify */
							int pleft = pad - xstr.length();

							if (pleft > 0) {
								byte spaces[] = new byte[pleft];

								for (int k=0; k<pleft; k++) {
									spaces[k] = ' ';
								}
								pstr = new String (spaces);
							} else {
								pstr = "";
							}
							outstr += (pstr + xstr);
						}
					} else {
						outstr += xstr;
					}
				} catch (Exception e) {
					return null;
				}
			}

		}

		return outstr;
	}
	//}}}
	//{{{  public static void setenv (MEnv envp, String name, String value)
	/**
	 * sets a value in a process environment
	 *
	 * @param envp environment in which to set
	 * @param name variable name being set
	 * @param value value
	 */
	public static void setenv (MEnv envp, String name, String value)
	{
		int i, slen;
		String srch;

		if (value == null) {
			value = "";
		}
		if ((name == null) || (envp == null) || (envp.env == null)) {
			return;
		}

		/* see if it's already here */
		name = name.toUpperCase ();
		srch = name + "=";
		slen = srch.length();
		for (i=0; i<envp.env.length; i++) {
			if (envp.env[i].substring (0, slen).equals (srch)) {
				break;		/* for() */
			}
		}

		if (i == envp.env.length) {
			/* not in environment, add it */
			String newenv[] = new String[envp.env.length + 1];

			System.arraycopy (envp.env, 0, newenv, 0, envp.env.length);
			envp.env = newenv;
		}
		envp.env[i] = name + "=" + value;
		return;
	}
	//}}}
	//{{{  public static String getenv (MEnv envp, String name)
	/**
	 * gets a value from a process environment
	 *
	 * @param envp environment in which to look
	 * @param name variable name to get
	 *
	 * @return value of the variable (copy), or null if not found
	 */
	public static String getenv (MEnv envp, String name)
	{
		int i, slen;
		String srch;

		if ((name == null) || (envp == null) || (envp.env == null)) {
			return null;
		}

		name = name.toUpperCase ();
		srch = name + "=";
		slen = srch.length ();
		for (i=0; i<envp.env.length; i++) {
			if (envp.env[i].substring (0, slen).equals (srch)) {
				return envp.env[i].substring (slen);
			}
		}
		return null;
	}
	//}}}
	//{{{  public static String []split_string (String str)
	/**
	 * splits a string up (based on whitespace)
	 *
	 * @param str string to chop up
	 *
	 * @return array of bits (maybe zero-sized)
	 */
	public static String []split_string (String str)
	{
		String bits[];
		int nbits;
		int i, slen;

		slen = str.length ();

		//{{{  count number of bits first
		for (i = 0; (i < slen) && ((str.charAt(i) == ' ') || (str.charAt(i) == '\t')); i++);
		nbits = 0;
		for (; i<slen;) {
			/* skip non-whitespace */
			for (; (i<slen) && ((str.charAt(i) != ' ') && (str.charAt(i) != '\t')); i++);
			nbits++;
			/* skip whitespace */
			for (; (i<slen) && ((str.charAt(i) == ' ') || (str.charAt(i) == '\t')); i++);
		}
		//}}}
		//{{{  break up into "bits"
		bits = new String[nbits];
		for (i = 0; (i < slen) && ((str.charAt(i) == ' ') || (str.charAt(i) == '\t')); i++);
		nbits = 0;
		for (; i<slen;) {
			int j;
			/* skip non-whitespace */
			for (j=i; (j<slen) && ((str.charAt(j) != ' ') && (str.charAt(j) != '\t')); j++);
			bits[nbits] = str.substring (i, j);
			nbits++;
			/* skip whitespace */
			for (i=j; (i<slen) && ((str.charAt(i) == ' ') || (str.charAt(i) == '\t')); i++);
		}
		//}}}
		return bits;
	}
	//}}}

	//{{{  public static int gethostbyname (String hostname, MSocketAddr addr)
	/**
	 * looks up a host by name
	 *
	 * @param hostname hostname to lookup
	 * @param addr populated with the address
	 *
	 * @return 0 on success, or &lt; 0 indicating error
	 */
	public static int gethostbyname (String hostname, MSocketAddr addr)
	{
		InetAddress ina;

		try {
			ina = InetAddress.getByName (hostname);
		} catch (UnknownHostException e) {
			return -MSystem.ENOENT;
		}
		addr.family = MSocketAddr.AF_INET;
		addr.address = ina.getAddress ();
		addr.port = 0;
		addr.path = null;

		return 0;
	}
	//}}}
	
	
}


