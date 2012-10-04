/*
 *	MFileSystem.java -- file-system handling for MOSS
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

package moss.fs;

import moss.kernel.*;
import moss.user.*;


/**
 * this class handles file-system management.  This is used to re-direct I/O
 * requests to the right driver, etc.
 */

public class MFileSystem
{
	//{{{  private vars
	/** this is used as a lock for static vars */
	private static Object synclock;

	/** file-system mount points ("/" always in index 0) */
	private static String mount_points[];
	/** file-system reference counts */
	private static int refcount[];
	/** file-system instances */
	private static MFSOps filesystems[];
	/** file-system names (fs type) */
	private static String mount_types[];
	/** file-system mount options */
	private static String mount_options[][];
	//}}}

	//{{{  private class PFS_mounts implements MProcFSIf
	/**
	 * this class provides the "mounts" information in the process file-system
	 */
	private static class PFS_mounts implements MProcFSIf
	{
		public String readproc (MInode inode, String name)
		{
			String r = "";
			int i;

			synchronized (synclock) {
				for (i=0; i<mount_points.length; i++) {
					if (mount_points[i] != null) {
						int j;

						r = r + MStdLib.sprintf ("%-3d %-12s %-20s  ", new Object[] {new Integer (refcount[i]), mount_types[i], mount_points[i]});
						for (j=0; j<mount_options[i].length; j++) {
							r = r + ((j == 0) ? "" : ",") + mount_options[i][j];
						}
						r = r + "\n";
					}
				}
			}

			return r;
		}
	}
	//}}}
	
	
	//{{{  public static void init_filesystem ()
	/**
	 * initialises the file-system bits
	 */
	public static void init_filesystem ()
	{
		synclock = new Object ();

		mount_points = new String[MConfig.max_mounted_fs];
		refcount = new int[MConfig.max_mounted_fs];
		filesystems = new MFSOps[MConfig.max_mounted_fs];
		mount_types = new String[MConfig.max_mounted_fs];
		mount_options = new String[MConfig.max_mounted_fs][];

		for (int i=0; i<MConfig.max_mounted_fs; i++) {
			mount_points[i] = null;
			refcount[i] = 0;
			filesystems[i] = null;
			mount_types[i] = null;
			mount_options[i] = null;
		}
		return;
	}
	//}}}
	//{{{  public static void finalinit ()
	/**
	 * called to perform final initialisation (in the context of the init-task)
	 */
	public static void finalinit ()
	{
		MProcFS.register_procimpl ("mounts", new PFS_mounts());

		return;
	}
	//}}}

	
	//{{{  private static int find_mount (String path)
	/**
	 * searches for the mount-point index where "path" is
	 *
	 * @param path absolute path to search for
	 *
	 * @return file-system index
	 */
	private static int find_mount (String path)
	{
		int idx, longest;

		if ((path.length() == 0) || (path.charAt(0) != '/')) {
			return -1;
		}
		idx = -1;
		longest = 0;
		for (int i=0; i<mount_points.length; i++) {
			if (mount_points[i] != null) {
				int mplen = mount_points[i].length();

				if ((path.length() < mplen) || (mplen < longest)) {
					continue;		/* to for() */
				}
				if (mount_points[i].equals (path.substring (0, mplen))) {
					longest = mplen;
					idx = i;
				}
			}
		}
		return idx;
	}
	//}}}
	//{{{  private static String split_mount (String path, int idx)
	/**
	 * removes the leading `mount' path component from a full path.
	 *
	 * @param path absolute path
	 * @param idx index of mount-point to which it belongs
	 *
	 * @return a string containing the path component relative to the mount-point.  This will return "/" instead
	 * 		of "", where that case arises.
	 */
	private static String split_mount (String path, int idx)
	{
		String tail;
		int plen = path.length ();

		if ((plen > 1) && (path.charAt(plen - 1) == '/')) {
			plen--;
		}
		if (plen == mount_points[idx].length()) {
			tail = "/";
		} else if (path.charAt (mount_points[idx].length()) == '/') {
			/* don't include leading / */
			tail = path.substring (mount_points[idx].length() + 1, plen);
		} else {
			tail = path.substring (mount_points[idx].length(), plen);
		}
		// MKernel.log_msg ("MFileSystem::split_path(): path = [" + path + "],  mount_points[idx] = [" + mount_points[idx] + "],  tail = [" + tail + "]");
		return tail;
	}
	//}}}

	//{{{  public static int mount (String path, String fstype, String options[])
	/**
	 * mounts a file-system
	 *
	 * @param path absolute path
	 * @param fstype file-system type
	 * @param options file-system specific options
	 *
	 * @return 0 on success, &lt; 0 on failure
	 */
	public static int mount (String path, String fstype, String options[])
	{
		Class fsclass;
		MFSOps fs;
		int pidx, tidx;

		try {
			fsclass = Class.forName ("moss.fs." + fstype);
		} catch (ClassNotFoundException e) {
			MKernel.log_msg ("failed to mount: " + e);
			return -MSystem.ENODEV;
		}

		/* check for valid path */
		if (path.charAt(0) != '/') {
			return -MSystem.EINVAL;
		}
		synchronized (synclock) {
			String tail;

			if (path.equals ("/")) {
				pidx = -1;
				tidx = 0;
				if (filesystems[0] != null) {
					return -MSystem.EBUSY;		/* root fs already mounted */
				}
			} else {
				pidx = find_mount (path);
				if (pidx < 0) {
					return -MSystem.ENOENT;		/* only if no root fs */
				}
				for (tidx=1; (tidx<filesystems.length) && (filesystems[tidx] != null); tidx++);
				if (tidx == filesystems.length) {
					return -MSystem.ENOMEM;		/* reached max. number of mounted file-systems */
				}
			}

			if (pidx >= 0) {
				tail = split_mount (path, pidx);
			} else {
				tail = new String(path);
			}
			/* check that mount-point is a directory */
			if (pidx >= 0) {
				MInode st_buf = new MInode ();
				int x;

				x = filesystems[pidx].stat (tail, st_buf);
				if (x < 0) {
					/* aborting.. */
					return x;
				}
				if ((st_buf.mode & MInode.S_IFMT) != MInode.S_IFDIR) {
					return -MSystem.ENOTDIR;
				}
			}
			/* setup in mounts and create */
			mount_points[tidx] = new String (path);
			mount_types[tidx] = new String (fstype);
			if (options == null) {
				mount_options[tidx] = new String[0];
			} else {
				mount_options[tidx] = (String[])options.clone ();
			}
			refcount[tidx] = 0;


			/* create a new instance of the desired file-system */
			try {
				fs = (MFSOps)(fsclass.newInstance());
			} catch (Exception e) {
				MKernel.log_msg ("MFileSystem::mount(): fstype [" + fstype + "] not MFSOps ?");
				return -MSystem.ENODEV;
			}

			/* oki, created, store and inform driver */
			filesystems[tidx] = fs;
			filesystems[tidx].mount (path, options);
		}

		return 0;
	}
	//}}}
	//{{{  public static int umount (String path)
	/**
	 * unmounts a file-system
	 *
	 * @param path absolute path to the mounted file-system
	 *
	 * @return 0 on success, or &lt;0 indicating error
	 */
	public static int umount (String path)
	{
		int didx = find_mount (path);
		int r;

		if (didx < 0) {
			return -MSystem.ENOENT;
		}
		synchronized (synclock) {
			r = filesystems[didx].umount (new String[] {"path=" + path});

			if (r == 0) {
				/* successful un-mount, remove from the filesystems table */
				mount_points[didx] = null;
				filesystems[didx] = null;
				refcount[didx] = 0;
				mount_types[didx] = null;
				mount_options[didx] = null;
			}
		}

		return r;
	}
	//}}}
	//{{{  public static int open (String path, MFile handle, int flags, int mode)
	/**
	 * opens a file
	 *
	 * @param path absolute path to the file
	 * @param handle file-handle that will be used for this file
	 * @param flags OPEN_READ / OPEN_WRITE / OPEN_CREAT
	 *
	 * @return 0 on success, otherwise &lt; 0 indicating error
	 */
	public static int open (String path, MFile handle, int flags, int mode)
	{
		int didx = find_mount (path);
		String rpath;

		if (didx < 0) {
			return -MSystem.ENOENT;
		}
		rpath = split_mount (path, didx);

		synchronized (synclock) {
			return filesystems[didx].open (rpath, handle, flags, mode);
		}
	}
	//}}}
	//{{{  public static int opendir (String path, MFile handle)
	/**
	 * opens a directory
	 *
	 * @param path absolute path to the directory
	 * @param handle file-handle that will be used for this directory
	 *
	 * @return 0 on success, otherwise &lt; 0 indicating error
	 */
	public static int opendir (String path, MFile handle)
	{
		int didx = find_mount (path);
		String rpath;

		if (didx < 0) {
			return -MSystem.ENOENT;
		}
		rpath = split_mount (path, didx);

		synchronized (synclock) {
			return filesystems[didx].opendir (rpath, handle);
		}
	}
	//}}}
	//{{{  public static int mkdir (String path, int flags)
        /**
	 * creates a directory in the file-system
	 *
	 * @param path relative path to the directory to be created
	 * @param flags flags (mode)
	 *
	 * @return 0 on success, or &lt; 0 indicating error
	 */
	public static int mkdir (String path, int flags)
	{
		int didx = find_mount (path);
		String rpath;

		if (didx < 0) {
			return -MSystem.ENOENT;
		}
		rpath = split_mount (path, didx);

		synchronized (synclock) {
			return filesystems[didx].mkdir (rpath, flags);
		}
	}
	//}}}
	//{{{  public static int stat (String path, MInode statbuf)
	/**
	 * stats a file/directory
	 *
	 * @param path absolute path to the file/directory
	 * @param statbuf MInode where the information will be placed
	 *
	 * @return 0 on success, otherwise &lt; 0 indicating error
	 */
	public static int stat (String path, MInode statbuf)
	{
		int didx = find_mount (path);
		String rpath;

		if (didx < 0) {
			return -MSystem.ENOENT;
		}
		rpath = split_mount (path, didx);
		synchronized (synclock) {
			return filesystems[didx].stat (rpath, statbuf);
		}
	}
	//}}}
	//{{{  public static int access (String path, int amode)
	/**
	 * tests for access to a file
	 *
	 * @param path absolute path to the file/directory
	 * @param amode access mode: bitwise-or of (MFileOps) F_OK, R_OK, W_OK and/or X_OK
	 *
	 * @return 0 on success, otherwise &lt; 0 indicating error
	 */
	public static int access (String path, int amode)
	{
		int didx = find_mount (path);
		String rpath;

		if (didx < 0) {
			return -MSystem.ENOENT;
		}
		rpath = split_mount (path, didx);
		synchronized (synclock) {
			return filesystems[didx].access (rpath, amode);
		}
	}
	//}}}
	//{{{  public static int unlink (String path)
	/**
	 * unlinks (removes) a file or directory
	 *
	 * @param path absolute path to the file/directory
	 *
	 * @return 0 on success, otherwise &lt; 0 indicating error
	 */
	public static int unlink (String path)
	{
		int didx = find_mount (path);
		String rpath;

		if (didx < 0) {
			return -MSystem.ENOENT;
		}
		rpath = split_mount (path, didx);
		synchronized (synclock) {
			return filesystems[didx].unlink (rpath);
		}
	}
	//}}}
}


