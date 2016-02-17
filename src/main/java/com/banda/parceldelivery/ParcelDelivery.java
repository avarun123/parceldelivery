package com.banda.parceldelivery;

import java.io.BufferedReader;
import java.io.FileReader;
import java.io.InputStreamReader;
import java.sql.Timestamp;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;

import org.apache.commons.math3.stat.descriptive.SummaryStatistics;
import org.jgrapht.UndirectedGraph;
import org.jgrapht.alg.DijkstraShortestPath;
import org.jgrapht.graph.DefaultEdge;
import org.jgrapht.graph.DefaultWeightedEdge;
import org.jgrapht.graph.SimpleDirectedWeightedGraph;
import org.jgrapht.graph.SimpleGraph;
import org.jgrapht.graph.SimpleWeightedGraph;

public class ParcelDelivery {

	static   	Map<String,Map<String,Activity>> userActivityMap = new HashMap<>();
	static Map<String,Map<String,Integer>> towerToUserCount = new HashMap<String,Map<String,Integer>>();
	static DateFormat df = new SimpleDateFormat("yyyy-MM-dd hh:mm:ss");
	public static void main(String[] args) {
		// TODO Auto-generated method stub
		try {
			//readInput("C:\\projects\\upwork\\PhilipBanda\\cellphone.csv");
			readInput(args[0]);
		} catch(Exception e) {
			e.printStackTrace( );
		}
	}
	
	public static void readInput(String inputfile) throws Exception {
		BufferedReader in = new BufferedReader(new FileReader(inputfile));
		String line = null;
		while((line=in.readLine())!=null) {
			String[] split = line.split(",");
			if(split.length < 10 )
				continue;
			String productNo=split[0].trim();
			String towerid=split[1].trim();
			String cellId=split[2].trim();
			String moment=split[3].trim();
		    String startTime=split[4].trim();
		    Date dt = df.parse(startTime);
		    String[] dateParts = startTime.split(" ");
			//String timestamp = time.mktime(datetime.datetime.strptime(startTime, "%Y-%m-%d H:%M:%S").timetuple())

			String day = dateParts[0];
			//String dayparts = day.split("-");
				        
				       
				         

		    String userId=split[5].trim();
		    String countyId=split[6].trim();
		    String IMEI=split[7].trim();
		    String stayTime=split[8].trim();
			String dist=split[9].trim();
			
			// keep a list off time stamps for each day for the user along with the tower id
			// sort the list in ascending order of time for each day
			// create a time diff for each adjacent towers for each user
			
			// take the average and create graph of towers
			// map with user as key. Value is a map with Date string as key and a list of activity as value
		
			Map<String,Activity> activityForUser = userActivityMap.get(userId);
			if( activityForUser == null){
				activityForUser = new HashMap<String,Activity>();
				userActivityMap.put(userId, activityForUser);
			}
			Activity firstActivityForTheDay = activityForUser.get(day);
			if(firstActivityForTheDay == null) {
				firstActivityForTheDay = new Activity(dt.getTime(), towerid, userId);
				activityForUser.put(day, firstActivityForTheDay);
			} else {
				// insert activity at the right position so that the list is always sorted
				Activity activity = new Activity(dt.getTime(), towerid, userId);
				if(activity.compareTo(firstActivityForTheDay) < 0){
					
					activity.next  = firstActivityForTheDay;
					activityForUser.put(day, activity); // make this as the first activity
				} else {
					// find the position of activity to insert in the linnked list
					Activity current = firstActivityForTheDay;
					Activity previous = null;
					while(current!=null) {
						// find the first activity which has occurred later than this activity
						if(current.compareTo(activity) > 0){
							// insert before current
							//Activity tmp = previous.next;
							previous.next = activity;
							activity.next = current;
							break;
						}
						previous = current;
						current = current.next;
					}
					if(current == null) {
						// insert at the end
						previous.next = activity;
					}
				}
			}
		}
		
		// now process each users activity for the day and compute the average time difference between adjacent towers
		 
		Map<String,SummaryStatistics> towerDistances = new HashMap<>();
		SummaryStatistics stats = new SummaryStatistics();
		for(Entry<String,Map<String,Activity>> e:userActivityMap.entrySet()) {
			String userId = e.getKey();
			Map<String,Activity> activitiesForEachDay = e.getValue();
			
			for(Entry<String,Activity> e1:activitiesForEachDay.entrySet()) {
				Set<String> userToTowerAssociated = new HashSet<>();
				Activity firstAcctivity = e1.getValue();
				Activity current = firstAcctivity;
				Activity previous = null;
				while(current!=null){
					
					// get a count of number of days each user is associated with a tower.
					 
					//if(!(userToTowerAssociated.contains(userId+","+current.towerId))) {
						userToTowerAssociated.add(userId+","+current.towerId);
						if(towerToUserCount.get(current.towerId) == null) {
							towerToUserCount.put(current.towerId, new HashMap<String,Integer>());
						}
						Map<String,Integer> userCount = towerToUserCount.get((current.towerId));
						if(userCount.get(userId) == null){
							userCount.put(userId, new Integer(1));
						} else {
							userCount.put(userId, new Integer(userCount.get(userId).intValue()+1));
						}
					//}
					if(current!=null && previous!=null) {
						String first = previous.towerId, second = current.towerId;
						if(previous.towerId.compareTo(current.towerId) > 0){
							first = current.towerId;
							second = previous.towerId;
						}
						if(towerDistances.get(first+";"+second) == null && !(first.equals(second))){
							towerDistances.put(first+";"+second, new SummaryStatistics());
							
						}
						
						if(!(first.equals(second)) )
						towerDistances.get(first+";"+second).addValue((current.timeStamp - previous.timeStamp)/1000);
					}
					previous = current;
					current = current.next;
				}
			}
		}
		
		// find the users who are mostly associated with a tower
		Map<String,String> towerToUserMap = new HashMap<>();
		for(String tower:towerToUserCount.keySet()) {
			int max = Integer.MIN_VALUE;
			Map<String,Integer> map = towerToUserCount.get(tower);
			for(String user:map.keySet()) {
				if(map.get(user)!=null) {
					if(map.get(user).intValue() > max) {
						max = map.get(user).intValue();
						towerToUserMap.put(tower, user);
					}
				}
			}
		}
		// we have tower distances in towerDistances table. Use this as a graph to find the shortest paths
		SimpleWeightedGraph<String, DefaultWeightedEdge> towergraph = new SimpleWeightedGraph<String, DefaultWeightedEdge>(DefaultWeightedEdge.class);
		Set<String> alreadyAdded = new HashSet<>();
		for(Entry<String,SummaryStatistics> e:towerDistances.entrySet()) {
			String[] towerpair = e.getKey().split(";");
			if(!alreadyAdded.contains(towerpair[0])) {
			towergraph.addVertex(towerpair[0]);
			alreadyAdded.add(towerpair[0]);
			}
			if(!alreadyAdded.contains(towerpair[1])) {
				towergraph.addVertex(towerpair[1]);
				alreadyAdded.add(towerpair[1]);
			}
			DefaultWeightedEdge e1 = towergraph.addEdge(towerpair[0], towerpair[1]); 
			towergraph.setEdgeWeight(e1, e.getValue().getMean()); 
		}
		 String input = "";
		while(input!=null && !input.toLowerCase().equals("q")) {
			System.out.println("Enter input in the format tower1,tower2");
			
			  in = new BufferedReader(new InputStreamReader(System.in));
			  input = in.readLine();
			  String[] split = input.split(",");
			  if(split.length!=2) {
				  System.out.println("Enter input in the format tower1,tower2");
				  continue;
			  }
			

			  if(!towergraph.containsVertex(split[0])) {
				 System.out.println("Tower "+split[0]+" Does not exist. Input a valid pair of towers");
				 continue;
			 }
			 if(!towergraph.containsVertex(split[1])) {
				 System.out.println("Tower "+split[1]+" Does not exist. Input a valid pair of towers");
				 continue;
			 }
			 List<DefaultWeightedEdge> path= DijkstraShortestPath.findPathBetween(towergraph, split[0], split[1]);
			 System.out.println("Printing shortest path between the towers "+split[0]+" and " +split[1]+ " Along with users to be assigned at each tower");
			 for(DefaultWeightedEdge edge:path){
				 System.out.println(edge+","+towergraph.getEdgeWeight(edge)+" Users at tower "+towergraph.getEdgeSource(edge)+"  = "+towerToUserMap.get(towergraph.getEdgeSource(edge))+" User at tower "+towergraph.getEdgeTarget(edge)+" = "+towerToUserMap.get(towergraph.getEdgeTarget(edge)));
			 }
		}
	}
	
	public static class Activity implements Comparable{
		long timeStamp;
		String towerId;
		String userid;
		Activity next;
		@Override
		
		public int compareTo(Object that){
			Activity thatActivity = (Activity) that;
			if (this.timeStamp > thatActivity.timeStamp)
				return 1;
			else if (this.timeStamp < thatActivity.timeStamp)
				return -1;
			else
				return 0;
						
			
		}
		public Activity(long timeStamp, String towerId, String userid) {
			super();
			this.timeStamp = timeStamp;
			this.towerId = towerId;
			this.userid = userid;
		}
		
	}

}
