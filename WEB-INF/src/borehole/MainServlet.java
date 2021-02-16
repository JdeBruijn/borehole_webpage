/*
	Jon de Bruijn
	2021-02-09
	This class gets and sets values in the database in accordance with requests from the front-end.
*/

package borehole;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;

import java.io.IOException;

import javax.servlet.http.HttpSession;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import java.util.logging.Logger;
import java.util.Calendar;
import java.util.LinkedList;

import org.json.simple.JSONObject;
import org.json.simple.JSONArray;

public class MainServlet extends HttpServlet
{
	private static final String class_name = "MainServlet";
	private static final Logger log = Logger.getLogger(class_name);

	private static final int tank_capacity=5000;

	private static final long day_s = 60*60*24;
	private static final long timezone_offset = 60*60*2;

	private static final String[] week_day_names = new String[] {"Sunday", "Monday", "Tuesday", "Wednesday", "Thursday", "Friday", "Saturday"};


	public void doGet(HttpServletRequest req, HttpServletResponse resp)
	{
		String oper = req.getHeader("oper");

		if(oper.contentEquals("get_status_data"))
		{getStatusData(req, resp);}
		else if(oper.contentEquals("getWaterLevelGraphData"))
		{getWaterLevelGraphData(req, resp);}
		else if(oper.contentEquals("getWaterConsumptionGraphData"))
		{getWaterConsumptionGraphData(req, resp);}
		else if(oper.contentEquals("getLogs"))
		{getLogs(req, resp);}
	}//doGet().

	public void doPost(HttpServletRequest req, HttpServletResponse resp)
	{

	}//doPost().

	private void getStatusData(HttpServletRequest req, HttpServletResponse resp)
	{
		/*
		+-------------+----------------------+
		| log_code_id | log_code_description |
		+-------------+----------------------+
		|           1 | Device restarted     |
		|           2 | Borehole pump start  |
		|           3 | Borehole pump stop   |
		|           4 | Tank Full            |
		|           5 | Booster pump started |
		|           6 | Booster pump stopped |
		|          21 | Critical Error       |
		|          22 | Warning              |
		+-------------+----------------------+
		*/
		long current_epoch = System.currentTimeMillis()/1000;
		long day_s = 60*60*24;
		long current_day_epoch = (current_epoch/day_s)*day_s;
		long day_ago_epoch = current_day_epoch-day_s;
		String get_status_data = "SELECT borehole_log_message, borehole_log_pump,FROM_UNIXTIME(borehole_log_time) AS log_date"
							+ " FROM borehole_log"
							+ " WHERE borehole_log_time>="+day_ago_epoch
				//			+ " WHERE borehole_log_time>=1612697671"//debug**
							+ " AND borehole_log_code>=20"
							+ " ORDER BY borehole_log_time DESC";

		Connection conn = DatabaseHelper.getConnection();
		try
		{
			log.info(class_name+" get_status_data = "+get_status_data);//debug**
			ResultSet res = conn.prepareStatement(get_status_data).executeQuery();
			boolean got_borehole_state=false;
			String borehole_message="OK";
			boolean got_booster_state=false;
			String booster_message="OK";
			while(res.next())
			{
				if(!got_borehole_state && res.getInt("borehole_log_pump")==1)
				{
					got_borehole_state=true;
					borehole_message=res.getString("borehole_log_message")+" "+res.getString("log_date");
				}//if.
				else if(!got_booster_state && res.getInt("borehole_log_pump")==2)
				{
					got_booster_state=true;
					booster_message=res.getString("borehole_log_message")+" "+res.getString("log_date");
				}//else if.

				if(got_booster_state && got_borehole_state)
				{break;}
			}//while.

			JSONObject json_data = new JSONObject();
			json_data.put("borehole_ok", !got_borehole_state);
			json_data.put("borehole_status", borehole_message);
			json_data.put("booster_ok", !got_booster_state);
			json_data.put("booster_status", booster_message);
			returnData(true, "", json_data, resp);
		}//try
		catch(SQLException se)
		{
			log.severe(class_name+" SQL Exception while trying to get status data:\n"+se);
			returnData(false, "Failed to get status data.", null, resp);
			return;
		}//catch().
		finally
		{
			try
			{conn.close();}
			catch(NullPointerException | SQLException se)
			{log.severe(class_name+" Exception while trying to close db connection in getStatusData():\n"+se);}
		}//finally.

	}//getStatusData().

	private void getWaterLevelGraphData(HttpServletRequest req, HttpServletResponse resp)
	{
		Calendar calendar = Calendar.getInstance();

		String message=null;


		long current_epoch = System.currentTimeMillis()/1000;
		//long current_epoch = 1612749604l;//debug**
		//calendar.setTimeInMillis(current_epoch*1000);//debug**
		long current_day_epoch = (current_epoch/day_s)*day_s;
		long week_s = 7*day_s;
		long two_weeks_ago_epoch = current_day_epoch-(2*week_s);
		long one_week_ago_epoch = current_day_epoch-(1*week_s);

		int current_day_of_week = calendar.get(Calendar.DAY_OF_WEEK);
		log.info(class_name+" current_day_of_week = "+current_day_of_week+" current_day_epoch="+current_day_epoch);//debug**
		System.out.println("two_weeks_ago_epoch="+two_weeks_ago_epoch+" one_week_ago_epoch="+one_week_ago_epoch);//debug**
		int week_start_day = current_day_of_week+1;
		if(week_start_day>7)//loop around.
		{week_start_day=1;}

		String get_last_full_log = "SELECT borehole_log_id, borehole_log_message, FROM_UNIXTIME(borehole_log_time)"
						+ " FROM borehole_log"
						+ " WHERE borehole_log_code=4"
						+ " AND borehole_log_time<"+two_weeks_ago_epoch
						+ " ORDER BY borehole_log_time DESC"
						+ " LIMIT 1";

		String get_graph_data = "SELECT DAYNAME(FROM_UNIXTIME(borehole_log_time)) AS day_name, DAYOFWEEK(FROM_UNIXTIME(borehole_log_time)) AS day_of_week,"
						+ " DATE(FROM_UNIXTIME(borehole_log_time)) AS date, borehole_log_time,"
						+ " SUM(borehole_log_pump_volume) AS volume, borehole_log_pump, borehole_log_code"
						+ " FROM borehole_log"
						+ " WHERE borehole_log_id>?"
						+ " AND (borehole_log_pump_volume IS NOT NULL OR borehole_log_code=4)"//log_code==4 => tank full.
						+ " AND ((borehole_log_code=6 AND borehole_log_pump_time<450) OR borehole_log_code!=6)"//When ZESA is off it thinks booster is pumping.
						+ " GROUP BY date, borehole_log_pump"
						+ " ORDER BY date ASC";
		Connection conn = DatabaseHelper.getConnection();
		try
		{
			log.info(class_name+" get_last_full_log = "+get_last_full_log);//debug**
			ResultSet res = conn.prepareStatement(get_last_full_log).executeQuery();
			long last_full_log_id=0;
			if(res.next())
			{last_full_log_id = res.getLong("borehole_log_id");}
			else
			{
				returnData(false, "Failed to get graph data.", null, resp);
				return;
			}//else.

			PreparedStatement stmt = conn.prepareStatement(get_graph_data);
			stmt.setLong(1, last_full_log_id);
			log.info(class_name+" get_graph_data = "+stmt);//debug**

			LinkedList<int[]> recorded_volumes = new LinkedList<int[]>();//format: [ [day_of_week, volume], ...].

			res = stmt.executeQuery();
			int prev_volume = tank_capacity;
			int water_volume = tank_capacity;
			int week_day=week_start_day;

			log.info(class_name+" week_start_day = "+week_start_day);//debug**

			int prev_recorded_day=week_start_day-1;
			if(prev_recorded_day<1)
			{prev_recorded_day=7;}

			long prev_recorded_epoch=two_weeks_ago_epoch;
			long prev_epoch=0l;
			int prev_day=-1;
			int cur_day=-1;
			log.info(class_name+" day_s="+day_s);//debug**
			while(res.next())
			{
				if(res.getInt("borehole_log_code")==4)//Tank reached full again.
				{
					water_volume=tank_capacity;
					continue;
				}//if.

				cur_day=res.getInt("day_of_week");

				long log_epoch = ((res.getLong("borehole_log_time")+timezone_offset)/day_s)*day_s;

				if(log_epoch!=prev_epoch && prev_epoch>=(two_weeks_ago_epoch+day_s))//Start recording if log is from within the last 2 weeks.
				{
					log.info(class_name+" prev_epoch="+prev_epoch+" two_weeks_ago_epoch+day_s="+(two_weeks_ago_epoch+day_s));//debug**
					log.info(class_name+" prev_day="+prev_day+" prev_recorded_day="+prev_recorded_day);//debug**
					recordVolume(prev_recorded_epoch, prev_epoch, prev_volume, water_volume, recorded_volumes);
					//recorded_volumes.add(new int[] {prev_day, water_volume});
					prev_recorded_epoch=prev_epoch;
					prev_recorded_day=prev_day;
				}//if.

				prev_epoch=log_epoch;
				prev_day=cur_day;
				log.info(class_name+" prev_day="+prev_day+" prev_epoch="+prev_epoch);//debug**
				prev_volume=water_volume;

				int volume_change = res.getInt("volume");
				log.info(class_name+" volume_change = "+volume_change);//debug**
				if(res.getInt("borehole_log_pump")==1)//water pumped into tank.
				{water_volume+=volume_change;}
				else//water pumped out of tank.
				{water_volume-=volume_change;}

				if(water_volume>tank_capacity)//Obviously the water volume can't be greater than the tank capacity so something's gone wrong.
				{
					water_volume=tank_capacity;
					message="Seems booster pump data is missing. Most likely because controller was powered off for some reason.";
				}//if.
				else if(water_volume<0)//Obviously the water volume can't be than 0 so something's gone wrong.
				{
					water_volume=tank_capacity;
					message="Seems borehole pump data is missing. This should never happen! Please check system.";
				}//if.
			}//while.
			recordVolume(prev_recorded_epoch, prev_epoch, prev_volume, water_volume, recorded_volumes);

			//Split recorded_volumes into last_week[] and this_week[] and create labels[]
			JSONArray last_week = new JSONArray();
			JSONArray this_week = new JSONArray();

			StringBuilder labels = new StringBuilder();

			int day=0;
			for(int[] volume_data: recorded_volumes)
			{
			//	if(day>13)//debug**
			//	{break;}

				log.info(class_name+" day of week = "+volume_data[0]);//debug**

				if(day<=6)
				{
					last_week.add(volume_data[1]);
					if(day>0)
					{labels.append(",");}
					labels.append(week_day_names[volume_data[0]-1]);
				}//if.
				else
				{this_week.add(volume_data[1]);}

				day++;
			}//for(volume_data).

			log.info(class_name+" last_week = "+last_week.toString());//debug**
			log.info(class_name+" this_week = "+this_week.toString());//debug**
			log.info(class_name+" labels = "+labels.toString());//debug**

			JSONObject json_data = new JSONObject();
			json_data.put("last_week",last_week);
			json_data.put("this_week",this_week);
			json_data.put("labels",labels.toString());
			returnData(true, message, json_data, resp);
		}//try
		catch(SQLException se)
		{
			log.severe(class_name+" SQL Exception while trying to get graph data:\n"+se);
			returnData(false, "Failed to get graph data.", null, resp);
			return;
		}//catch().
		finally
		{
			try
			{conn.close();}
			catch(NullPointerException | SQLException se)
			{log.severe(class_name+" Exception while trying to close db connection in getWaterLevelGraphData():\n"+se);}
		}//finally.

	}//getWaterLevelGraphData().

	private void recordVolume(long prev_recorded_epoch, long prev_epoch, int prev_volume, int water_volume,LinkedList<int[]> recorded_volumes)
	{	
		log.info(class_name+" prev_epoch="+prev_epoch+" prev_recorded_epoch="+prev_recorded_epoch);//debug**
		Calendar calendar = Calendar.getInstance();					
		while((prev_recorded_epoch+day_s)<prev_epoch)
		{
			prev_recorded_epoch+=day_s;
			System.out.println(" prev_recorded_epoch="+prev_recorded_epoch+" prev_epoch="+prev_epoch);//debug**
			calendar.setTimeInMillis(prev_recorded_epoch*1000);
			int day = calendar.get(Calendar.DAY_OF_WEEK);
			recorded_volumes.add(new int[] {day, prev_volume});//set volume for 'missing' day = last recorded volume.
		}//while.

		calendar.setTimeInMillis(prev_epoch*1000);
		int day = calendar.get(Calendar.DAY_OF_WEEK);
		recorded_volumes.add(new int[] {day, water_volume});
	}//recordVolume().

	private void getWaterConsumptionGraphData(HttpServletRequest req, HttpServletResponse resp)
	{
		Calendar calendar = Calendar.getInstance();

		String message=null;


		long current_epoch = System.currentTimeMillis()/1000;
		//long current_epoch = 1612749604l;//debug**
		//calendar.setTimeInMillis(current_epoch*1000);//debug**
		long current_day_epoch = (current_epoch/day_s)*day_s;
		long week_s = 7*day_s;
		long two_weeks_ago_epoch = current_day_epoch-(2*week_s);
		long one_week_ago_epoch = current_day_epoch-(1*week_s);

		int current_day_of_week = calendar.get(Calendar.DAY_OF_WEEK);
		log.info(class_name+" current_day_of_week = "+current_day_of_week+" current_day_epoch="+current_day_epoch);//debug**
		System.out.println("two_weeks_ago_epoch="+two_weeks_ago_epoch+" one_week_ago_epoch="+one_week_ago_epoch);//debug**
		int week_start_day = current_day_of_week+1;
		if(week_start_day>7)//loop around.
		{week_start_day=1;}

		String get_consumption_data = "SELECT DAYNAME(FROM_UNIXTIME(borehole_log_time)) AS day_name, DAYOFWEEK(FROM_UNIXTIME(borehole_log_time)) AS day_of_week,"
						+ " DATE(FROM_UNIXTIME(borehole_log_time)) AS date, borehole_log_time,"
						+ " SUM(borehole_log_pump_volume) AS volume, borehole_log_pump, borehole_log_code"
						+ " FROM borehole_log"
						+ " WHERE borehole_log_time>="+(two_weeks_ago_epoch+day_s)
						+ " AND borehole_log_code=6"//log_code==4 => tank full.
						+ " AND borehole_log_pump_time<450"//When ZESA is off it thinks booster is pumping.
						+ " GROUP BY date"
						+ " ORDER BY date ASC";

		Connection conn = DatabaseHelper.getConnection();
		try
		{
			PreparedStatement stmt = conn.prepareStatement(get_consumption_data);
			log.info(class_name+" get_consumption_data = "+stmt);//debug**

			LinkedList<int[]> recorded_volumes = new LinkedList<int[]>();//format: [ [day_of_week, volume], ...].

			ResultSet res = stmt.executeQuery();
			int used_volume = 0;
			int week_day=week_start_day;

			log.info(class_name+" week_start_day = "+week_start_day);//debug**

			long prev_recorded_epoch=two_weeks_ago_epoch;
			long cur_epoch=0l;
			int cur_day=-1;
			log.info(class_name+" day_s="+day_s);//debug**
			while(res.next())
			{
				cur_day=res.getInt("day_of_week");

				used_volume = res.getInt("volume");

				cur_epoch = ((res.getLong("borehole_log_time")+timezone_offset)/day_s)*day_s;

				recordVolume(prev_recorded_epoch, cur_epoch, 0, used_volume, recorded_volumes);
				//recorded_volumes.add(new int[] {prev_day, water_volume});
				prev_recorded_epoch=cur_epoch;
			}//while.
			recordVolume(prev_recorded_epoch, cur_epoch, 0, used_volume, recorded_volumes);

			//Split recorded_volumes into last_week[] and this_week[] and create labels[]
			JSONArray last_week = new JSONArray();
			JSONArray this_week = new JSONArray();

			StringBuilder labels = new StringBuilder();

			int day=0;
			for(int[] volume_data: recorded_volumes)
			{
			//	if(day>13)//debug**
			//	{break;}

				log.info(class_name+" day of week = "+volume_data[0]);//debug**

				if(day<=6)
				{
					last_week.add(volume_data[1]);
					if(day>0)
					{labels.append(",");}
					labels.append(week_day_names[volume_data[0]-1]);
				}//if.
				else
				{this_week.add(volume_data[1]);}

				day++;
			}//for(volume_data).

			log.info(class_name+" last_week = "+last_week.toString());//debug**
			log.info(class_name+" this_week = "+this_week.toString());//debug**
			log.info(class_name+" labels = "+labels.toString());//debug**

			JSONObject json_data = new JSONObject();
			json_data.put("last_week",last_week);
			json_data.put("this_week",this_week);
			json_data.put("labels",labels.toString());
			returnData(true, message, json_data, resp);
		}//try
		catch(SQLException se)
		{
			log.severe(class_name+" SQL Exception while trying to get graph data:\n"+se);
			returnData(false, "Failed to get graph data.", null, resp);
			return;
		}//catch().
		finally
		{
			try
			{conn.close();}
			catch(NullPointerException | SQLException se)
			{log.severe(class_name+" Exception while trying to close db connection in getWaterConsumptionGraphData():\n"+se);}
		}//finally.

	}//getWaterConsumptionGraphData().


	private void getLogs(HttpServletRequest req, HttpServletResponse resp)
	{
		long current_epoch = System.currentTimeMillis()/1000;
		//long current_epoch = 1612749604l;//debug**
		//calendar.setTimeInMillis(current_epoch*1000);//debug**
		long current_day_epoch = (current_epoch/day_s)*day_s;
		long week_s = 7*day_s;
		long one_day_ago_epoch = current_day_epoch-(1*day_s);


		String get_logs = "SELECT log_code_description AS message, borehole_log_pump_volume, borehole_log_pump_time, borehole_log_pump, FROM_UNIXTIME(borehole_log_time) AS log_date, borehole_log_code"
					+ " FROM borehole_log"
					+ " JOIN log_codes ON log_code_id=borehole_log_code"
					+ " WHERE borehole_log_time>="+one_day_ago_epoch
					+ " ORDER BY log_date DESC"
					+ " LIMIT 200";

		Connection conn = DatabaseHelper.getConnection();
		try
		{
			log.info(class_name+" get_logs = "+get_logs);//debug**
			ResultSet res = conn.prepareStatement(get_logs).executeQuery();

			JSONObject json_data = new JSONObject();
			JSONArray borehole_values = new JSONArray();
			JSONArray booster_values = new JSONArray();
			while(res.next())
			{
				String date = res.getString("log_date");
				String message = res.getString("message");
				int pump = res.getInt("borehole_log_pump");
				int volume = res.getInt("borehole_log_pump_volume");
				int time = res.getInt("borehole_log_pump_time");
				int log_code = res.getInt("borehole_log_code");

				String log_string = date+","+message+","+volume+","+time+","+log_code;

				if(pump==1 || pump==0)//borehole logs.
				{borehole_values.add(log_string);}
				if(pump==2 || pump==0)//borehole logs.
				{booster_values.add(log_string);}
			}//while.

		//	System.out.println(class_name+" borehole_values="+borehole_values);//debug**
		//	System.out.println(class_name+" booster_values="+booster_values);//debug**

			json_data.put("borehole_headers","Date, Message, Volume(l), Time(s)");
			json_data.put("borehole_values", borehole_values);
			json_data.put("booster_headers","Date, Message, Volume(l), Time(s)");
			json_data.put("booster_values", booster_values);
			returnData(true, "", json_data, resp);
			return;
		}//try
		catch(SQLException se)
		{
			log.severe(class_name+" SQL Exception while trying to get logs:\n"+se);
			returnData(false, "Failed to retrieve logs.", null, resp);
			return;
		}//catch().
		finally
		{
			try
			{conn.close();}
			catch(NullPointerException | SQLException se)
			{log.severe(class_name+" Exception while trying to close db connection in getLogs():\n"+se);}
		}//finally.
	}//getLogs().


	private void returnData(boolean success, String message, JSONObject data, HttpServletResponse resp)
	{
		if(data==null)
		{data = new JSONObject();}

		if(!success)
		{log.warning(class_name+" returnData(): success="+success+" message= "+message);}

		data.put("success", success);
		data.put("message", message);

		resp.setContentType("text/json");
		try
		{resp.getWriter().println(data.toString());}
		catch(IOException ioe)
		{log.severe(class_name+" IO Exception while trying to return data:\n"+ioe);}
	}//returnData().


}//class MainServlet.




