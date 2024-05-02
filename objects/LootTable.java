package atavism.agis.objects;

import atavism.server.util.*;
import java.util.Random;
import java.util.ArrayList;

public class LootTable {
    public LootTable() {
    }
    
    public LootTable(int id, String name, ArrayList<Integer> items, ArrayList<Integer> itemCounts, ArrayList<Float> itemChances) {
    	this.id = id;
    	this.name = name;
    	this.items = items;
    	this.itemCounts = itemCounts;
    	this.itemChances = itemChances;
    }
    
    public int getID() { return id;}
    public void setID(int id) {
    	this.id = id;
    }
    
    public String getName() { return name;}
    public void setName(String name) {
    	this.name = name;
    }
    
      
    public ArrayList<Integer> getItems() { return items;}
    public void setItems(ArrayList<Integer> items) {
    	this.items = items;
    }
    
    public ArrayList<Integer> getItemCounts() { return itemCounts;}
    public void setItemCounts(ArrayList<Integer> itemCounts) {
    	this.itemCounts = itemCounts;
    }
  
    public ArrayList<Integer> getItemMaxCounts() { return itemMaxCounts;}
    public void setItemMaxCounts(ArrayList<Integer> itemMaxCounts) {
    	this.itemMaxCounts = itemMaxCounts;
    }
  
    public ArrayList<Float> getItemChances() { return itemChances;}
    public void setItemChances(ArrayList<Float> itemChances) {
    	this.itemChances = itemChances;
    }
    
    public int getTotalRollChance() {
    	int totalRollChance = 0;
    	for (int i = 0; i < itemChances.size(); i++) {
    		totalRollChance += itemChances.get(i);
    	}
    	return totalRollChance;
    }
    
   
    public int getNewRandomItemNum() {
    	Random rand = new Random();
    	float roll = rand.nextInt(10000)/100;
    	if (Log.loggingDebug)Log.debug("LOOT: generating random number for table: " + id + ". Roll is: " + roll + "; with " + itemChances.size() + " items "+this);
    	for (int i = 0; i < itemChances.size(); i++) {
    		if (Log.loggingDebug)Log.debug("LOOT: item pos=" + i + " Id="+items.get(i)+ " itemChance=" +  itemChances.get(i) + " no vipChance roll="+roll);
    		if(itemChances.get(i)  >= roll ) {
    			ArrayList<Integer> sameChance = new ArrayList<Integer>();
    			sameChance.add(i);
    			for(int j = 0; j < itemChances.size(); j++) {
    				if (Log.loggingDebug)Log.debug("LOOT: item pos=" + i + " and pos "+j+" "+itemChances.get(i) +" "+ itemChances.get(j));
    				if(i!=j && Float.compare(itemChances.get(i), itemChances.get(j))==0) {
    					if (Log.loggingDebug)Log.debug("LOOT: chance same");
    					sameChance.add(j);
    				}else {
    					if (Log.loggingDebug)Log.debug("LOOT: chance not same");
    				}

    			}
    			if(sameChance.size()>1) {
    				int idx = rand.nextInt(sameChance.size());
    				if (Log.loggingDebug)	Log.debug("LOOT: item same chance idx "+idx+" "+sameChance);
    				return sameChance.get(idx);
    			}
    			return i;
    		}
    	}
    	return -1;
    }
   
    
    public int getNewRandomItemNum(float vipChance) {
    	Random rand = new Random();
    	float roll = rand.nextInt(10000)/100;
    	if (Log.loggingDebug)Log.debug("LOOT: generating random number for table: " + id + ". Roll is: " + roll + "; with " + itemChances.size() + " items "+this);
    	for (int i = 0; i < itemChances.size(); i++) {
    		if (Log.loggingDebug)	Log.debug("LOOT: item pos=" + i + " Id="+items.get(i)+ " itemChance=" +  itemChances.get(i) + " vipChance="+ vipChance+" calculated chance "+(itemChances.get(i) + itemChances.get(i) * vipChance)+"roll="+roll);
			if (itemChances.get(i) + itemChances.get(i) * vipChance / 100F >= roll) {
    			ArrayList<Integer> sameChance = new ArrayList<Integer>();
    			sameChance.add(i);
    			for(int j = 0; j < itemChances.size(); j++) {
    				if (Log.loggingDebug)Log.debug("LOOT: item pos=" + i + " and pos "+j+" "+itemChances.get(i) +" "+ itemChances.get(j));
    				if(i!=j && Float.compare(itemChances.get(i), itemChances.get(j))==0) {
    					if (Log.loggingDebug)Log.debug("LOOT: chance same");
    					sameChance.add(j);
    				}else {
    					if (Log.loggingDebug)	Log.debug("LOOT: chance not same");
    				}
    			}
    			if(sameChance.size()>1) {
    				int idx = rand.nextInt(sameChance.size());
    				if (Log.loggingDebug)Log.debug("LOOT: item same chance idx "+idx+" "+sameChance);
    				return sameChance.get(idx);
    			}
    			return i;
    		}
    	}
    	return -1;
    }
    
	public int getRandomCountOfItem(int id) {
		if (items.size() > id) {
			if (itemMaxCounts.get(id) > itemCounts.get(id)) {
				Random rand = new Random();
				return itemCounts.get(id) + rand.nextInt(itemMaxCounts.get(id) - itemCounts.get(id));
			} else {
				return itemCounts.get(id);
			}
		}
		return 0;
	}
    
    
    public String toString() {
    	return "[LootTable "+id + " " + name + " items="+items+" itemChances="+itemChances+" itemCounts="+itemCounts+" itemMaxCounts="+itemMaxCounts+"]";
    }

    int id;
    String name;
   // int itemsPerLoot = 1;
    ArrayList<Integer> items = new ArrayList<Integer>();
    ArrayList<Integer> itemCounts = new ArrayList<Integer>();
    ArrayList<Integer> itemMaxCounts = new ArrayList<Integer>();
     ArrayList<Float> itemChances = new ArrayList<Float>();

    private static final long serialVersionUID = 1L;
}
