/*using System;
using System.Collections.Generic;
using System.Linq;
using System.Text;
using System.IO;
using System.Collections;
using System.Threading;*/
package vadimsuraev.LogUtility;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.Collection;
import java.util.Date;
import java.util.Dictionary;
import java.util.HashMap;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.Queue;
import java.util.concurrent.Semaphore;
import java.util.concurrent.locks.ReentrantLock;

import vadimsuraev.ReferencedTypes.ReferencedInteger;

public class LogUtility extends Thread
{
  	public enum LogLevels
    {
        LEVEL_LOG_NONE,
        LEVEL_LOG_MEDIUM,
        LEVEL_LOG_HIGH,
        LEVEL_LOG_HIGH2,
        LEVEL_LOG_HIGH3
    };
    public static String FileName = "log.txt";
    static LinkedList<String> logEntriesList = new LinkedList<String>();
    static LogUtility logThread  =  null;
    private static boolean Silent = false;
    private static byte CurrentLevel = (byte)LogLevels.LEVEL_LOG_HIGH3.ordinal();
    static Semaphore sm = new Semaphore(-1);
    static ReentrantLock m_LogQueueMutex = new ReentrantLock();
    static  FileOutputStream m_fs = null;

    private static byte[] GetBytes(String str, ReferencedInteger reallLength)
    {
    	String separator = System.getProperty( "line.separator" );
        char[] charr = (str + separator).toCharArray();
        byte[] buff = new byte[charr.length * 2];

        for(int i = 0;i < charr.length;i++)
        {
            buff[i] = (byte)charr[i];
        }
        reallLength.val = charr.length;
        return buff;
    }
    public void run()
    {
    	try {
			m_fs = new FileOutputStream(FileName);
		} catch (FileNotFoundException e1) {
			// TODO Auto-generated catch block
			e1.printStackTrace();
		}
        while (true)
        {
            try {
				sm.acquire();
			} catch (InterruptedException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
            while (RetrieveEntryAndWrite(logEntriesList, FileName)) ;
        }
    }
    public static void SetSilent(Boolean isSilent)
    {
        Silent = isSilent;
    }
    public static void SetLevel(LogLevels level)
    {
        CurrentLevel = (byte)level.ordinal();
    }
    public static void Stop()
    {
        if (logThread != null)
        {
            logThread.stop();
        }
    }
    public static void LogFile(String entry, LogLevels level)
    {
        if (Silent)
        {
            return;
        }
        if (CurrentLevel > (byte)level.ordinal())
        {
            return;
        }
        entry = Long.toString((new Date()).getTime()) + " " + Long.toString(Thread.currentThread().getId()) + " " + entry;
        m_LogQueueMutex.lock();
        logEntriesList.add(entry);
        if(logThread == null)
        {
            logThread = new LogUtility();
            logThread.start();
        }
        m_LogQueueMutex.unlock();
        sm.release();
    }
    public static void LogException(String prefix,Exception exc,LogLevels level)
    {
    	String str = prefix + " " + exc.getMessage();
    	String separator = System.getProperty( "line.separator" );
    	StackTraceElement []stack = exc.getStackTrace();
    	if(stack != null)
    	{
    		for(int i = 0;i < stack.length;i++)
    		{
    			str += separator + stack[i].getFileName() + ":" + stack[i].getMethodName() + ":" + Integer.toString(stack[i].getLineNumber());
    		}
    	}
    	str += separator;
    	LogFile(str,level);
    }
    static boolean RetrieveEntryAndWrite(LinkedList<String> entriesList,String filename)
    {
        String entry = null;
        m_LogQueueMutex.lock();
        if (entriesList.size() != 0)
        {
            entry = (String)entriesList.removeFirst();
        }
        m_LogQueueMutex.unlock();
        if (entry != null)
        {
            ReferencedInteger reallLength = new ReferencedInteger();
            byte[] buff = GetBytes(entry, reallLength);
            try {
				m_fs.write(buff, 0, reallLength.val);
				m_fs.flush();
	            //fs.close();
			} catch (IOException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
            entry = null;
            return true;
        }
        return false;
    }
}
