/**
 * This file is licensed to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */


package com.testing.notifications;
import java.util.*;
import javax.mail.*;
import javax.mail.internet.*;
//import javax.activation.*;

public class notify {

	public static String from = "";
	/**
	 * @param args
	 */
	public static boolean sendMail (String from,  String to, String text) {
		
		String host = "";
		/*
		 * userid and password required only if authentication is 
		 * required by the MTA
		 */
		String userid = "";
		String password = "";
		Properties props = new Properties();
		props.put("mail.smtp.starttls.enable", "true"); 
		props.put("mail.smtp.host", host); 
		props.put("mail.smtp.port", "465"); 
		props.put("mail.smtps.auth", "true");
		props.setProperty("mail.transport.protocol", "smtps");
		props.put("mail.smtp.user", userid); 
		props.put("mail.smtp.password", password);  
		props.put("mail.debug", "true");
		Session session = Session.getInstance(props);
		
		try{
			Message msg  = new MimeMessage(session);
			msg.setFrom(new InternetAddress(from));
			InternetAddress[] address = {new InternetAddress(to)};
			msg.setRecipients(Message.RecipientType.TO, address);
			InternetAddress address1  = new InternetAddress(from);
			msg.setFrom(address1);
			msg.setSubject("Test email from the flume notifier");
			msg.setSentDate(new Date());
			
			msg.setText(text);
			Transport transport = session.getTransport("smtps");
			transport.connect(host, userid, password);
			transport.sendMessage(msg, msg.getAllRecipients());
			transport.close();
			//Transport.send(msg);
		}
		catch (MessagingException mex){
			mex.printStackTrace();
		}
		return true;

	}

}
