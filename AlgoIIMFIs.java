/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package IIMFIsAlgorithm;

import ca.pfv.spmf.tools.MemoryLogger;
import java.io.BufferedReader;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
/**
 * @author Hussein Alsaeedi
 */
class PD_IMFIs {
     int item_Name = -1;
     int cur_Sup = 1;
     int incr_Sup = 1;
     int incr_Minsup = 0;
     List<IMFIs> list_IMFIs = null;
    PD_IMFIs() {
    }
}
class IMFIs {
    Integer[] IMFIs;
    int Support;
     IMFIs(Integer item) {
        IMFIs = new Integer[]{item};
    }
     IMFIs(Integer[] items) {
        this.IMFIs = items;
    }
     IMFIs(List<Integer> IMFIs, int support) {
        this.IMFIs = new Integer[IMFIs.size()];
        int i = 0;
        for (int item : IMFIs) {
            this.IMFIs[i++] = item;
        }
        this.Support = support;
    }
     int getAbsoluteSupport() {
        return Support;
    }
     int size() {
        return IMFIs.length;
    }
     int get(int position) {
        return IMFIs[position];
    }
     void setAbsoluteSupport(int support) {
        this.Support = support;
    }
}
public class AlgoIIMFIs {
    long startTimestamp; // start time of the latest execution
    long endTime; // end time of the latest execution
    double sizeMemory;
    int trnsNum = 0; // transaction count in the database
    int IMFIs; // number of IMFIs found
    double min_Sup;
    double minNovlty = 0;
    boolean useNM=false;
    int cur_Minsup;
    List<PD_IMFIs> PD_IMFIsnew = null; // Pi+1
    Map<Integer, Integer> MapIndexPDIMFIs = new HashMap<Integer, Integer>(); //save index for each item
    UpdatedTree updated_tree = null;
    final int BUFFERS_SIZE = 2000;
    private Integer[] itemsetBuffer = null;
    public MFITree mfiTree = null; // This is the MFI tree for storing maximal itemsets
    /**
     * *
     * @param DBname //Di+1
     * @param PD_IMFIsPrv //Pi
     * @param useNM //true or False
     * @param min_Sup//enter by user, >=0 and <=1
     * @param minNovlty //enter by user, >=0 and <=1
     * @return PD_IMFIsnew //Pi+1
     * @throws IOException
     */
    //Main IIMFIs Algorithm (Algorithm 1)
    public List<PD_IMFIs> runAlgorithm(String DBname, List<PD_IMFIs> PD_IMFIsPrv, boolean useNM, double min_Sup, double minNovlty) throws IOException {
        trnsNum = 0;  // number of transctrions in dataset found
        IMFIs = 0; // number of IMFIs found
        startTimestamp = System.currentTimeMillis(); // record start time
        MemoryLogger.getInstance().reset();//initialize tool to record memory usage
        MemoryLogger.getInstance().checkMemory();
        this.min_Sup = min_Sup;
        //Phase 1: Create PD_IMFIs as Pi+1 and incremental support and minimum threshold
        //1.  Pi+1 = Call Created PD_IMFIs Algorithm (Di+1, Pi ) 
        PD_IMFIsnew = CreatedPD_IMFIs(PD_IMFIsPrv, DBname); //call Algorithm 2
        //    Phase 2: Create tree, it includes previous IMFIs and transactions in Di+1   
        //5.	tree=null // as structure FP-tree 
        updated_tree = new UpdatedTree();
        //6.	tree = Call Created Tree Algorithm (Pi+1, Di+1, cur_Minsup) 
        CreatedUpdatedTree(DBname);
        //   Phase 3: Find MFIs from tree based on FP-Max algorithm
        //7.	MFI-tree=null 
        //8.	For each item from bottom to up in Pi+1 as P // where from bottom to up as header table with tree
        this.useNM=useNM;
        this.minNovlty=minNovlty;
        mfiTree = new MFITree();
        if (updated_tree.headerList.size() > 0) {
            itemsetBuffer = new Integer[BUFFERS_SIZE];
            fpMax(updated_tree, itemsetBuffer, 0, trnsNum, MapIndexPDIMFIs);
        }
        //PrintPD_IMFIs(PD_IMFIsnew);
        return PD_IMFIsnew;
    }

    // Algorithm 2  CreatedPD_IMFIs Algorithm
    private List<PD_IMFIs> CreatedPD_IMFIs(List<PD_IMFIs> PD_IMFIsPrv, String DBname) throws FileNotFoundException, IOException {
        //1.	Pi+1=Pi // Read PD_IMFIsPrv;
        for (int i = 0; i < PD_IMFIsPrv.size(); i++) {
            PD_IMFIs P = PD_IMFIsPrv.get(i);
            P.cur_Sup = 0;
            MapIndexPDIMFIs.put(P.item_Name, i);
        }
        //2.  first scan current database Di+1
        BufferedReader reader = new BufferedReader(new FileReader(DBname));
        String line;
        // 2. For each transaction T in Di+1
        while (((line = reader.readLine()) != null)) {
            if (line.isEmpty() == true || line.charAt(0) == '#' || line.charAt(0) == '%' || line.charAt(0) == '@') {
                continue;
            }
            String[] lineSplited = line.split(" ");
            //2.1  For each item I in T
            for (String itemString : lineSplited) {
                Integer item = Integer.parseInt(itemString);
                Integer hindex = MapIndexPDIMFIs.get(item);
                //2.1.1 If I is equal  item_Name for each item in Pi+1    as P Then 
                if (hindex != null) {
                    //i. Increase value of P. cur_Sup, and P. incr_Sup by 1
                    PD_IMFIsPrv.get(hindex).cur_Sup++;
                    PD_IMFIsPrv.get(hindex).incr_Sup++;
                }//2.1.2. Else
                else {
                    //i. Create a new item as Pnew with default value fields item_Name =I, cur_Sup =1, incr_Sup=1, incr_Minsup =0, and list_IMFIs=null
                    PD_IMFIs Pnew = new PD_IMFIs();
                    Pnew.item_Name = item;
                    Pnew.cur_Sup = 1;
                    Pnew.incr_Sup = 1;
                    Pnew.list_IMFIs = new ArrayList<>();
                    //ii. Add a Pnew to 1-Itemset list in Pi+1
                    PD_IMFIsPrv.add(Pnew);
                    MapIndexPDIMFIs.put(item, PD_IMFIsPrv.size() - 1);
                } // 2.1.3 End if 
            } //2.2 End for
            trnsNum++; // increase the transaction count
        } //3. End For   // close the input file
        reader.close();
        //2.  cur_Minsup = Min_Sup * n 
        cur_Minsup = (int) Math.ceil(min_Sup * trnsNum);
        // 4. Re-sort Pi+1 in descending order by the value of incr_Sup filed.
        List<Integer> FList = new ArrayList<Integer>(MapIndexPDIMFIs.keySet());
        Collections.sort(FList, new Comparator<Integer>() {
            public int compare(Integer id1, Integer id2) {
                int compare = PD_IMFIsPrv.get(MapIndexPDIMFIs.get(id2)).incr_Sup - PD_IMFIsPrv.get(MapIndexPDIMFIs.get(id1)).incr_Sup;
                if (compare == 0) {
                    compare = (id1 - id2);
                    return compare;
                }
                return compare;
            }
        });
        List<PD_IMFIs> PD_IMFIsnow = new ArrayList<>();
        HashMap<Integer, Integer> MapIndexPDIMFIsnew = new HashMap<Integer, Integer>();
        int i = 0;
        for (Integer item : FList) {
            Integer indx = MapIndexPDIMFIs.get(item);
            PD_IMFIs Pnew = PD_IMFIsPrv.get(indx);
            Pnew.incr_Minsup += cur_Minsup;
            if (Pnew.incr_Sup >= Pnew.incr_Minsup || Pnew.cur_Sup >= cur_Minsup) {
                if (Pnew.list_IMFIs.size() > 0) {
                    BulidTreeFromIMFIs(Pnew, PD_IMFIsPrv);
                }
                Pnew.list_IMFIs = new ArrayList<>();
            } else {
                IMFIs += Pnew.list_IMFIs.size();
            }
            PD_IMFIsnow.add(Pnew);
            MapIndexPDIMFIsnew.put(item, i);
            i++;
        }
        MapIndexPDIMFIs = MapIndexPDIMFIsnew;
        //5. Returm Pi+1
        return PD_IMFIsnow;
    }

    private void BulidTreeFromIMFIs(PD_IMFIs P, List<PD_IMFIs> modelPrv) {
        for (IMFIs listIMFIs : P.list_IMFIs) {
            //Add all itemsets in IMFI to a new array as arrayTrns,line 3.
            List<Integer> newTrans = new ArrayList<>();
            newTrans.addAll(Arrays.asList(listIMFIs.IMFIs));
            //Sort arrayTrns as new MIFMIs M1+1, line 4.
            Collections.sort(newTrans, new Comparator<Integer>() {
                public int compare(Integer item1, Integer item2) {
                    // compare the support
                    //  int compare = MapIndexHList.get(item1) - MapIndexHList.get(item2);
                    int compare = modelPrv.get(MapIndexPDIMFIs.get(item2)).incr_Sup - modelPrv.get(MapIndexPDIMFIs.get(item1)).incr_Sup;
                    // if the same support, we check the lexical ordering!
                    if (compare == 0) {
                        return item1.compareTo(item2);
                    }
                    // otherwise use the support
                    return compare;
                }
            });
            //   Call  FP-Growth algorithm to build updated-tree as FP-tree structure with arrayTrns, and new IIMFIs Mi+1, line 5. Note here, if an item in arrayTrns is not in the same path in the updated-tree, create new node with counter equal to previous support prv_Sup, accounted by (incr_Sup – now_Sup) for each item of arrayTrns equal to Item_name in new IIMFIs Mi+1, else, …..  don’t increase nodes counter, line 7 end for line 2, reset of list_IMFIs  by null in line 8. Line 9 end for line 1.
            //updated_tree.addMIFIsToupdatedtree(newTrans, IMFIs.support);
            //  IMFIs=null;
        }
    }

    private void CreatedUpdatedTree(String input) throws IOException {
        //Step 2.2
        BufferedReader reader = new BufferedReader(new FileReader(input));
        String line;
        while (((line = reader.readLine()) != null)) {
            if (line.isEmpty() == true
                    || line.charAt(0) == '#' || line.charAt(0) == '%'
                    || line.charAt(0) == '@') {
                continue;
            }
            String[] lineSplited = line.split(" ");
            List<Integer> transaction = new ArrayList<Integer>();
            for (String itemString : lineSplited) {
                Integer item = Integer.parseInt(itemString);
                Integer indx = MapIndexPDIMFIs.get(item);
                if (PD_IMFIsnew.get(indx).incr_Sup >= PD_IMFIsnew.get(indx).incr_Minsup || PD_IMFIsnew.get(indx).cur_Sup >= cur_Minsup) {
                    transaction.add(item);
                }
            }
            Collections.sort(transaction, new Comparator<Integer>() {
                public int compare(Integer item1, Integer item2) {
                    int compare = MapIndexPDIMFIs.get(item1) - MapIndexPDIMFIs.get(item2);
                    if (compare == 0) {
                        return item1.compareTo(item2);
                    }
                    return compare;
                }
            });
            updated_tree.addTransaction(transaction);
        }         // close the input file
        reader.close();
        updated_tree.createHeaderList(MapIndexPDIMFIs);
    }
    private void PrintPD_IMFIs(List<PD_IMFIs> PD_IMFIsnow) {
        for (PD_IMFIs Pnow : PD_IMFIsnow) {
            System.out.println(Pnow.item_Name + " sup:" + Pnow.incr_Sup);
        }
    }
    private void fpMax(UpdatedTree tree, Integer[] prefix, int prefixLength, int prefixSupport, Map<Integer, Integer> mapSupport) throws IOException {
        boolean singlePath = true;
        int singlePathSupport = 0;
        int minSupportRelative = 0;
        int position = prefixLength;
        if (tree.root.childs.size() > 1) {
            singlePath = false;
        } else {
            FPNode currentNode = tree.root.childs.get(0);
            while (true) {
                if (currentNode.childs.size() > 1) {
                    singlePath = false;
                    break;
                }
                itemsetBuffer[position] = currentNode.itemID;
                singlePathSupport = currentNode.counter;
                position++;
                if (currentNode.childs.isEmpty()) {
                    break;
                }
                currentNode = currentNode.childs.get(0);
            }
        }
        if (singlePath && singlePathSupport >= minSupportRelative) {
            saveItemset(itemsetBuffer, position, singlePathSupport);
        } else {
            for (int i = tree.headerList.size() - 1; i >= 0; i--) {
                Integer item = tree.headerList.get(i);
                Integer indx = this.MapIndexPDIMFIs.get(item);
                if (PD_IMFIsnew.get(indx).incr_Sup >= PD_IMFIsnew.get(indx).incr_Minsup || PD_IMFIsnew.get(indx).cur_Sup >= this.cur_Minsup) {
                    int support = PD_IMFIsnew.get(indx).incr_Sup;
                    minSupportRelative = PD_IMFIsnew.get(indx).incr_Minsup;
                    prefix[prefixLength] = item;
                    int betaSupport = (prefixSupport < support) ? prefixSupport : support;
                    List<List<FPNode>> prefixPaths = new ArrayList<List<FPNode>>();
                    FPNode path = tree.mapItemNodes.get(item);
                    Map<Integer, Integer> mapSupportBeta = new HashMap<Integer, Integer>();
                    while (path != null) {
                        if (path.parent.itemID != -1) {
                            List<FPNode> prefixPath = new ArrayList<FPNode>();
                            prefixPath.add(path);
                            int pathCount = path.counter;
                            FPNode parent = path.parent;
                            while (parent.itemID != -1) {
                                prefixPath.add(parent);
                                if (mapSupportBeta.get(parent.itemID) == null) {
                                    mapSupportBeta.put(parent.itemID, pathCount);
                                } else {
                                    mapSupportBeta.put(parent.itemID, mapSupportBeta.get(parent.itemID) + pathCount);
                                }
                                parent = parent.parent;
                            }
                            prefixPaths.add(prefixPath);
                        }
                        path = path.nodeLink;
                    }
                    // ===== FPMAX ======
                    List<Integer> headWithP = new ArrayList<Integer>(mapSupportBeta.size() + prefixLength + 1);
                    for (int z = 0; z < prefixLength + 1; z++) {
                        headWithP.add(prefix[z]);
                    }
                    for (Entry<Integer, Integer> entry : mapSupportBeta.entrySet()) {
                        if (entry.getValue() >= minSupportRelative) {
                            headWithP.add(entry.getKey());
                        }
                    }
                    Collections.sort(headWithP, comparatorOriginalOrder);
                    if (mfiTree.passSubsetChecking(headWithP)) {
                        UpdatedTree treeBeta = new UpdatedTree();
                        for (List<FPNode> prefixPath : prefixPaths) {
                            treeBeta.addPrefixPath(prefixPath, mapSupportBeta, minSupportRelative);
                        }
                        if (treeBeta.root.childs.size() > 0) {
                            treeBeta.createHeaderList(MapIndexPDIMFIs);
                            fpMax(treeBeta, prefix, prefixLength + 1, betaSupport, mapSupportBeta);
                        }
                        // ======= After that, we still need to check if beta is a maximal itemset ====
                        List<Integer> temp = new ArrayList<Integer>(mapSupportBeta.size() + prefixLength + 1);
                        for (int z = 0; z < prefixLength + 1; z++) {
                            temp.add(prefix[z]);
                        }
                        Collections.sort(temp, comparatorOriginalOrder);
                        if (mfiTree.passSubsetChecking(temp)) {
                            saveItemset(prefix, prefixLength + 1, betaSupport);
                        }
                        //===========================================================
                    }
                }
            }
        }
    }
    public void sortOriginalOrder(Integer[] a, int length) {
        for (int i = 0; i < length; i++) {
            for (int j = length - 1; j >= i + 1; j--) {
                boolean test = comparatorOriginalOrder.compare(a[j], a[j - 1]) < 0;
                if (test) {
                    Integer temp = a[j];
                    a[j] = a[j - 1];
                    a[j - 1] = temp;
                }
            }
        }
    }
    Comparator<Integer> comparatorOriginalOrder = new Comparator<Integer>() {
        public int compare(Integer item1, Integer item2) {
            int compare = MapIndexPDIMFIs.get(item1) - MapIndexPDIMFIs.get(item2);
            if (compare == 0) {
                return item1.compareTo(item2);
            }
            return compare;
        }
    };
    private void saveItemset(Integer[] itemset, int itemsetLength, int support) throws IOException {
        Integer[] itemsetCopy = new Integer[itemsetLength];
        System.arraycopy(itemset, 0, itemsetCopy, 0, itemsetLength);
        sortOriginalOrder(itemsetCopy, itemsetLength);
        mfiTree.addMFI(itemsetCopy, itemsetCopy.length, support);
        Integer indx = MapIndexPDIMFIs.get(itemsetCopy[itemsetLength - 1]);
        PD_IMFIs hList = PD_IMFIsnew.get(indx);
        Integer[] itemsetArray = new Integer[itemsetLength];
        System.arraycopy(itemset, 0, itemsetArray, 0, itemsetLength);
        Arrays.sort(itemsetArray);
        IMFIs MFInew = new IMFIs(itemsetArray);
        MFInew.setAbsoluteSupport(support);
         if (useNM == false) {
                       hList.list_IMFIs.add(MFInew);
            IMFIs++;
        } else {
             //Phase 4: Incremental Dynamic Pruning of MFIs
            if (IntrstingFIMnew(MFInew) >= this.minNovlty) {
                            hList.list_IMFIs.add(MFInew);
                IMFIs++;
            }
        }
      }
    private double IntrstingFIMnew(IMFIs MFIsnew) {
        double NM = 1;
        for (PD_IMFIs Pnew : PD_IMFIsnew) {

            if (Pnew.list_IMFIs.isEmpty()) {
                continue;
            }
            for (IMFIs MFIs : Pnew.list_IMFIs) {

                int S1 = MFIs.IMFIs.length;
                int S2 = MFIsnew.IMFIs.length;
                int k = 0;
                              k = FindValueK(MFIsnew.IMFIs, MFIs.IMFIs);
                double NMtemp = ((S1 + S2) - (2 * k));
                NMtemp = NMtemp / (S1 + S2);
                              if (NMtemp < NM) {
                    NM = NMtemp;
                }
            }
        }
        return NM;
    }
    private int FindValueK(Integer[] itemsetJ, Integer[] itemsetI) {
            int k = 0;
        if (itemsetJ.length > itemsetI.length) {
            for (Integer itemJ : itemsetJ) {
                for (Integer itemI : itemsetI) {
                    if (itemJ == null ? itemI == null : itemJ.equals(itemI)) {
                        k++;
                        break;
                    }
                }
            }
        } else {
            for (Integer itemI : itemsetI) {
                for (Integer itemJ : itemsetJ) {
                    if (itemJ == null ? itemI == null : itemJ.equals(itemI)) {
                        k++;
                        break;
                    }
                }
            }
        }
        return k;
    }
    public void printStats() {
        System.out.println("=============  HAS- STATS =============");
        long temps = endTime - startTimestamp;
        System.out.println(" Transactions count from database : " + trnsNum);
        System.out.print(" Max memory usage: " + MemoryLogger.getInstance().getMaxMemory() + " mb \n");
        System.out.println(" Maximal frequent itemset count : " + IMFIs);
        System.out.println(" Total time ~ " + temps + " ms");
        System.out.println("===================================================");
    }
}