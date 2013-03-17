package de.digitalemil.hadoop;

import java.io.IOException;
import java.util.List;
import java.util.Properties;

import org.apache.hadoop.fs.FSDataInputStream;
import org.apache.hadoop.fs.FSDataOutputStream;
import org.apache.hadoop.fs.FileSystem;
import org.apache.hadoop.fs.Path;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.support.ClassPathXmlApplicationContext;
import org.springframework.data.gemfire.support.GemfireCache;

import com.gemstone.gemfire.cache.Declarable;
import com.gemstone.gemfire.cache.EntryEvent;
import com.gemstone.gemfire.cache.asyncqueue.AsyncEvent;
import com.gemstone.gemfire.cache.util.CacheListenerAdapter;
import com.gemstone.gemfire.internal.cache.GemFireCacheImpl;

public class AsyncEventListener extends CacheListenerAdapter implements
		com.gemstone.gemfire.cache.asyncqueue.AsyncEventListener, Declarable {
	public static final String filename = "locations.txt";
	FileSystem fs;
	FSDataOutputStream out;
	static int trials= 0;
	
	public void init(Properties arg0) {
		ClassPathXmlApplicationContext ctx = new ClassPathXmlApplicationContext(
				"beans.xml");
		fs = ctx.getBean(org.apache.hadoop.fs.FileSystem.class);
		
		Path filenamePath = new Path("iicaptain/"+filename+"-"+System.currentTimeMillis()+"-"+System.getProperty("nodename"));
		try {
			if (fs.exists(filenamePath)) {
				fs.delete(filenamePath);
			}
			out = fs.create(filenamePath);
		
		} catch (Exception ioe) {
			System.err.println("IOException during operation: "
					+ ioe.toString());
		}
		trials++;
	}

	@Override
	public boolean processEvents(List<AsyncEvent> events) {
		try {
			for (AsyncEvent event : events) {
				if(!event.getOperation().isCreate())
				 continue;
				String eKey = (String) event.getKey();
				String val= event.getDeserializedValue().toString();
				out.writeUTF(eKey + ","+val+"\n");
				out.flush();
				out.sync();	
				System.out.println("Wrote to hadoop: "+eKey+","+val);
			}
		} catch (Exception ioe) {
			try {
				Thread.currentThread().sleep(100);
			} catch (InterruptedException e) {
			}
			if(trials< 10)
				init(null);
			return false;
		}
		trials= 0;
		return true;
	}
}
