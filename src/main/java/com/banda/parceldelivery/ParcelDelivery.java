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
	static Map<String,Map<Integer,Map<String,Integer>>> towerToUserCount = new HashMap<String,Map<Integer,Map<String,Integer>>>();
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
				firstActivityForTheDay = new Activity(dt.getTime(), towerid, userId,getNearestHour(dt),Integer.parseInt(stayTime));
				activityForUser.put(day, firstActivityForTheDay);
			} else {
				// insert activity at the right position so that the list is always sorted
				Activity activity = new Activity(dt.getTime(), towerid, userId,getNearestHour(dt),Integer.parseInt(stayTime));
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
		 
		//Map<String,Map<String,SummaryStatistics>> usersToTowerPairs = new HashMap<>(); // map to associate a tower pair with a user. key tower1-tower2, value user id
		Map<String,Map<Integer,Map<String,Integer>>> usersToTowerPairsCount = new HashMap<String,Map<Integer,Map<String,Integer>>>();
		Map<String,Map<Integer,Map<String,Integer>>> towerPairsToUserCount = new HashMap<String,Map<Integer,Map<String,Integer>>>();
		Map<String,SummaryStatistics> towerDistances = new HashMap<>();
		SummaryStatistics stats = new SummaryStatistics();
		Map<String,Map<Integer,Map<String,Integer>>> userToTowerCount = new HashMap<String,Map<Integer,Map<String,Integer>>>();
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
					 
					   int hourOfDay = current.hourOfDay;
					   addToTimestampedTowerAndUSerMap(towerToUserCount, current.towerId, hourOfDay, userId, 1);
					   addToTimestampedTowerAndUSerMap(towerToUserCount, current.towerId, -1, userId, 1); // -1 for all hours
						//userToTowerAssociated.add(userId+","+current.towerId);

						
					   addToTimestampedTowerAndUSerMap(userToTowerCount, userId, hourOfDay, current.towerId, 1);
					   addToTimestampedTowerAndUSerMap(userToTowerCount, userId, -1, current.towerId, 1); // -1 for all hours
					if(current!=null && previous!=null) {
						String first = previous.towerId, second = current.towerId;
						if(previous.towerId.compareTo(current.towerId) > 0){
							first = current.towerId;
							second = previous.towerId;
						}
						 
						if(towerDistances.get(first+";"+second) == null && !(first.equals(second))){
							towerDistances.put(first+";"+second, new SummaryStatistics());
							//usersToTowerPairs.put(first+";"+second, new HashMap<String,SummaryStatistics>());
						}
						
						if(!(first.equals(second)) ) {
							//towerDistances.get(first+";"+second).addValue((current.timeStamp - previous.timeStamp)/1000);
							towerDistances.get(first+";"+second).addValue(current.stayTimeInSeconds);
							addToTimestampedTowerAndUSerMap(towerPairsToUserCount, first+";"+second, hourOfDay, userId, 1);
							addToTimestampedTowerAndUSerMap(usersToTowerPairsCount, userId, hourOfDay, first+";"+second, 1);
							
							addToTimestampedTowerAndUSerMap(towerPairsToUserCount, first+";"+second, -1, userId, 1); // -1 for all hours
							addToTimestampedTowerAndUSerMap(usersToTowerPairsCount, userId, -1, first+";"+second, 1);// -1 for all hours
//							if(usersToTowerPairs.get(first+";"+second).get(userId) == null) {
//								usersToTowerPairs.get(first+";"+second).put(userId,new SummaryStatistics());
//							}
//							usersToTowerPairs.get(first+";"+second).get(userId).addValue(1);
						}
					}
					previous = current;
					current = current.next;
				}
			}
		}
		
		// find the users who are mostly associated with a tower for each hour of the day
		Map<Integer,Map<String,String>> towerToUserMap = updateWithMaxValueForEachHour(towerToUserCount);
		
		Map<Integer,Map<String,String>>  userToTowerMap =  updateWithMaxValueForEachHour(userToTowerCount);
		
		Map<Integer,Map<String,String>>  userToTowerPairsMap =  updateWithMaxValueForEachHour(usersToTowerPairsCount);
		
		Map<Integer,Map<String,String>>  towerPairsToUserMap =  updateWithMaxValueForEachHour(towerPairsToUserCount);
		 
		//Map<String,String> towerPairToUserMap = new HashMap<>();
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
			  System.out.println("Enter input in the format userid1,userid2,date time (format yyyy/mm/dd hh:mm)");
			  DateFormat df = new SimpleDateFormat("yyyy-MM-dd hh:mm");
			  in = new BufferedReader(new InputStreamReader(System.in));
			  input = in.readLine();
			  String[] split = input.split(",");
			  if(split.length!=3) {
				  System.out.println("Enter input in the format userid1,userid2,date time (format yyyy/mm/dd hh:mm)");
				  continue;
			  }
			
			  Date startDate = df.parse(split[2]);
			  int hour = getNearestHour(startDate);
			  
              Map<String,String> userToTowerMapForTheHour = userToTowerMap.get(hour);
              if(userToTowerMapForTheHour == null)
            	  userToTowerMapForTheHour = userToTowerMap.get(-1); // get the map for all the hours
              String tower1 = userToTowerMapForTheHour.get(split[0]);
              if(tower1 == null) {
            	  System.out.println("No cell tower found with user activity by user "+split[0]+" Enter a different start user");
            	  continue;
              }
              String tower2 = userToTowerMap.get(-1).get(split[1]);
              if(tower2 == null) {
            	  System.out.println("No cell tower found with user activity by user "+split[1]+" Enter a different end user");
            	  continue;
              }
			  if(!towergraph.containsVertex(tower1)) {
				 System.out.println("Tower "+tower1+" Does not exist. Input a valid pair of towers");
				 continue;
			 }
			 if(!towergraph.containsVertex(tower2)) {
				 System.out.println("Tower "+tower2+" Does not exist. Input a valid pair of towers");
				 continue;
			 }
			 List<DefaultWeightedEdge> path= DijkstraShortestPath.findPathBetween(towergraph, tower1, tower2);
			 //System.out.println("Printing shortest path between the users "+split[0]+" and " +split[1]+ " Along with users to be assigned at each intermediate path");
			 int i=0;
			 // compute the time to reach at each destination
			 long currentTime = startDate.getTime();
			 long previousTime = -1;
			 boolean towersApartByMoreThanAnHour = false;
			 System.out.println("Start from user "+ split[0]+" At tower "+tower1);
			 String lastUser = split[0];
			 String usesTowerSource = tower1;
			 String usersTowertarget = tower1;
			 String lastTowerTarget = tower1;
			 for(DefaultWeightedEdge edge:path){
				 i++;
				 String towerSource = towergraph.getEdgeSource(edge);
				 String towerTarget = towergraph.getEdgeTarget(edge);
				 if(towerTarget.equalsIgnoreCase(lastTowerTarget) && !lastTowerTarget.equalsIgnoreCase(tower1)) {
					 String tmp = towerSource;
					 towerSource = towerTarget;
					 towerTarget = tmp;
				 }
				 lastTowerTarget = towerTarget;
				 String first=towerTarget;String second = towerSource;
				 int secondsLapsed = (int)towergraph.getEdgeWeight(edge);
				 if(towerTarget.compareTo(towerSource) > 0)
				 {
					 first = towerSource;
					 second = towerTarget;
				 }
				 currentTime +=secondsLapsed * 1000;
				 int currenthour = getNearestHour(new Date(currentTime));
				  
				 if(previousTime!=-1 && currentTime - previousTime > 10 * 1000) {
					 towersApartByMoreThanAnHour = true;
					 // get a new user. Else within the same hour let the same user carry te parcel.
				 
					 Map<String,String> towerPairToUSerForTheHour = towerPairsToUserMap.get(currenthour);
					 if(towerPairToUSerForTheHour == null){
						 towerPairToUSerForTheHour=towerPairsToUserMap.get(-1);
					 }
					 String user = towerPairToUSerForTheHour.get(first+";"+second);
					 if(user == null){
						 user = towerPairsToUserMap.get(-1).get(first+";"+second);
					 }
					
	//				 if(user == null){
	//					 user = towerToUserMap.get(towerSource);
	//				 }
					 if(!user.equalsIgnoreCase(lastUser)) {
						 
						 System.out.println(i+" Hand over parcel to new user = "+user+ " at Time "+df.format(new Date(currentTime))+" at tower "+towerSource+" to travel to tower "+towerTarget);
					    //usesTowerSource = towerSource;
					 } else {
						 System.out.println(i+" User  = "+user+ " starting at Time "+df.format(new Date(currentTime))+" at tower "+towerSource+", travels to tower "+towerTarget);
					 }
					 lastUser = user;
				 }
				 previousTime = currentTime;
				
				 //System.out.println(edge+","+towergraph.getEdgeWeight(edge)+" Users at tower "+towergraph.getEdgeSource(edge)+"  = "+towerToUserMap.get(towergraph.getEdgeSource(edge))+" User at tower "+towergraph.getEdgeTarget(edge)+" = "+towerToUserMap.get(towergraph.getEdgeTarget(edge)));
			 }
			 
			 if(!towersApartByMoreThanAnHour) {
				 System.out.println("Travel distance betweeen the users are less than an hour. Source user can deliver directly to destination user");
			 }
			 System.out.println("End at user "+split[1]+ " at time "+df.format(new Date(currentTime))+" at Tower "+tower2);
		}
	}
	
	private static void addToTimestampedTowerAndUSerMap(Map<String,Map<Integer,Map<String,Integer>>> towerToUserCount,String topLevelKey,Integer hourOfDay,String secondLevelKey,int countValueToAdd) {
		if(towerToUserCount.get(topLevelKey) == null) {
			towerToUserCount.put(topLevelKey, new HashMap<Integer,Map<String,Integer>>());
		}
		Map<Integer,Map<String,Integer>> userCountForTheHour = towerToUserCount.get((topLevelKey));
		if(userCountForTheHour.get(hourOfDay) == null){
			Map<String,Integer> userCountMap =  new HashMap<String,Integer>();
			userCountMap.put(secondLevelKey, new Integer(countValueToAdd));
			userCountForTheHour.put(hourOfDay, userCountMap);
		} else {
			Map<String,Integer> userCountMap  = userCountForTheHour.get(hourOfDay);
			if(userCountMap.get(secondLevelKey) == null){
				userCountMap.put(secondLevelKey, new Integer(countValueToAdd));
			} else {
				userCountMap.put(secondLevelKey, new Integer(userCountMap.get(secondLevelKey).intValue()+countValueToAdd));
			}
		}
	}
	/**
	 * Method which returns the map for each hour with the value which has the maximum count for the hour
	 * @param towerToUserCount
	 * @return
	 */
	private static Map<Integer,Map<String,String>> updateWithMaxValueForEachHour(Map<String,Map<Integer,Map<String,Integer>>> towerToUserCount) {
		Map<Integer,Map<String,String>> retValue = new HashMap<>();
		for(String tower:towerToUserCount.keySet()) {
			
			Map<Integer,Map<String,Integer>> map1 = towerToUserCount.get(tower);
			for(Integer hour:map1.keySet()) {
				
				if(map1.get(hour)!=null) {
					if(retValue.get(hour) == null) {
						retValue.put(hour,new HashMap<String,String>());
					}
					int max = Integer.MIN_VALUE;
					Map<String,Integer> map2 = map1.get(hour);
					for(String user:map2.keySet()) {
						
					if(map2.get(user)!=null && map2.get(user).intValue() > max) {
						max = map2.get(user).intValue();
						retValue.get(hour).put(tower, user);
						//userToTowerMap.put(user, tower);
					}
					}
				}
			}
		}
		return retValue;
	}
	
	static int getNearestHour(Date date){
		int hour = date.getHours();
		int minute = date.getMinutes();
		if(minute > 30 )
			return getNextHour(hour);
		else
			return hour;
	}
	private static int getNextHour(int hour){
		if(hour == 23)
			return 0;
		else return hour+1;
	}
	public static class Activity implements Comparable{
		long timeStamp;
		String towerId;
		String userid;
		Activity next;
		int hourOfDay;
		int stayTimeInSeconds;
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
		public Activity(long timeStamp, String towerId, String userid,int hourOfDay,int stayTimeInSeconds) {
			super();
			this.timeStamp = timeStamp;
			this.towerId = towerId;
			this.userid = userid;
			this.hourOfDay = hourOfDay;
			this.stayTimeInSeconds = stayTimeInSeconds;
		}
		
	}

}
