/*
 *	MDevices.java -- device driver management
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

package moss.drivers;

import moss.fs.*;
import moss.kernel.*;
import moss.user.*;

/**
 * this class is used for device-driver management
 */

public class MDevices
{
	//{{{  private vars
	/** this acts as a lock for the static vars */
	private static Object synclock;

	/** device names */
	private static String dev_names[];
	/** device major numbers */
	private static int dev_major[];
	/** device driver references */
	private static MFileOps dev_drivers[];
	/** device driver reference counts */
	private static int dev_refcount[];
	//}}}  

	//{{{  private class PFS_devices implements MProcFSIf
	/**
	 * this class provides the contents of "devices" in the process file-system
	 */
	private static class PFS_devices implements MProcFSIf
	{
		public String readproc (MInode inode, String name)
		{
			String r;
			int i;

			r = "drivers loaded:\n";
			synchronized (synclock) {
				for (i=0; i<dev_names.length; i++) {
					if ((dev_names[i] != null) && (dev_major[i] > -1)) {
						r = r + MStdLib.sprintf ("  %-3d %-3d %s\n", new Object[] {new Integer(dev_major[i]),
							new Integer(dev_refcount[i]), ((dev_names[i] == null) ? "(none)" : dev_names[i])});
					}
				}
			}

			return r;
		}
	}
	//}}}

	//{{{  public static void init_devices ()
	/**
	 * called to initialise the device-driver management bits
	 */
	public static void init_devices ()
	{
		synclock = new Object ();

		dev_names = new String[MConfig.max_device_drivers];
		dev_major = new int[MConfig.max_device_drivers];
		dev_drivers = new MFileOps[MConfig.max_device_drivers];
		dev_refcount = new int[MConfig.max_device_drivers];

		for (int i=0; i<MConfig.max_device_drivers; i++) {
			dev_names[i] = null;
			dev_major[i] = -1;
			dev_drivers[i] = null;
			dev_refcount[i] = 0;
		}
	}
	//}}}
	//{{{  public static void finalinit ()
	/**
	 * called to perform final initialisation (sets up /proc things)
	 */
	public static void finalinit ()
	{
		MProcFS.register_procimpl ("devices", new PFS_devices());

		return;
	}
	//}}}

	//{{{  public static int find_driver (String name)
	/**
	 * finds a driver based on name (various uses)
	 *
	 * @param name device name
	 * 
	 * @return major device number, or -1 if not found
	 */
	public static int find_driver (String name)
	{
		int i;

		for (i=0; i<dev_names.length; i++) {
			if ((dev_names[i] != null) && (dev_names[i].equals (name))) {
				return dev_major[i];
			}
		}
		return -1;
	}
	//}}}

	//{{{  public static int register_driver (String name, int major, MFileOps drv)
	/**
	 * registers a device driver
	 *
	 * @param name device-driver name (must be unique)
	 * @param major major device number (must be unique)
	 * @param drv reference to device driver
	 *
	 * @return 0 on success, or &lt; 0 indicating error
	 */
	public static int register_driver (String name, int major, MFileOps drv)
	{
		int i;

		if ((name == null) || name.equals ("")) {
			return -MSystem.EINVAL;
		}
		if ((major < 0) || (major >= MConfig.max_device_drivers)) {
			return -MSystem.EINVAL;
		}

		synchronized (synclock) {
			if (dev_names[major] != null) {
				/* something already here */
				return -MSystem.EBUSY;
			}
			/* check the name isn't already used */
			for (i=0; i<dev_names.length; i++) {
				if ((dev_names[i] != null) && dev_names[i].equals (name)) {
					/* name already in use */
					return -MSystem.EEXISTS;
				}
			}

			/* register driver */
			dev_names[major] = new String (name);
			dev_major[major] = major;
			dev_drivers[major] = drv;
		}

		return 0;
	}
	//}}}
	//{{{  public static int unregister_driver (String name, int major)
	/**
	 * unregisters a device driver
	 *
	 * @param name device-driver name
	 * @param major major device number
	 *
	 * @return 0 on success, or &lt; 0 indicating error
	 */
	public static int unregister_driver (String name, int major)
	{
		if ((name == null) || name.equals ("")) {
			return -MSystem.EINVAL;
		}
		if ((major < 0) || (major >= MConfig.max_device_drivers)) {
			return -MSystem.EINVAL;
		}

		synchronized (synclock) {
			if (dev_refcount[major] > 0) {
				return -MSystem.EBUSY;
			}
			if ((dev_names[major] == null) || (dev_major[major] != major)) {
				return -MSystem.ENODEV;
			} else if (!dev_names[major].equals (name)) {
				return -MSystem.EINVAL;
			}

			/* unregister driver */
			dev_names[major] = null;
			dev_major[major] = -1;
			dev_drivers[major] = null;
		}
		return -MSystem.ENOSYS;
	}
	//}}}

	//{{{  public static int load_driver (String name)
	/**
	 * "dynamically" loads a device driver
	 *
	 * @param name class name containing the device-driver
	 *
	 * @return 0 on success, or &lt; 0 indicating error
	 */
	public static int load_driver (String name)
	{
		Class uclass;
		MFileOps driver;

		try {
			uclass = Class.forName (name);
		} catch (ClassNotFoundException e) {
			return -MSystem.ENOENT;
		}
		if (uclass == null) {
			return -MSystem.ENOENT;
		}

		try {
			driver = (MFileOps)(uclass.newInstance());
		} catch (Exception e) {
			return -MSystem.ENOEXEC;
		}
		/* driver registers itself, etc. */
		return 0;
	}
	//}}}

	//{{{  public static int open (MFile fh, int major, int minor, int flags)
	/**
	 * opens a device
	 *
	 * @param fh file-handle to be associated with the device (should have an inode attached)
	 * @param major major device number
	 * @param minor minor device number
	 * @param flags open flags
	 *
	 * @return 0 on success, otherwise &lt; 0 indicating error
	 */
	public static int open (MFile fh, int major, int minor, int flags)
	{
		if ((major < 0) || (major >= MConfig.max_device_drivers)) {
			return -MSystem.EINVAL;
		}
		if (fh.inode == null) {
			MKernel.panic ("MDevices::open() missing inode");
		}
		synchronized (synclock) {
			int result;

			if (dev_drivers[major] == null) {
				return -MSystem.ENODEV;
			}
			result = dev_drivers[major].open (fh, (flags & 0xffff) | (minor << 16));
			if (result < 0) {
				return result;
			}
			dev_refcount[major]++;
			fh.fileif = dev_drivers[major];
		}
		return 0;
	}
	//}}}
	//{{{  public static int close (MFile fh)
	/**
	 * closes a device
	 *
	 * @param fh file-handle associated with device
	 *
	 * @return 0 on success, otherwise &lt; 0 indicating error
	 */
	public static int close (MFile fh)
	{
		return -MSystem.ENOSYS;
	}
	//}}}

}


