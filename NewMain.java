/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package IIMFIsAlgorithm;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

/**
 *
 * @author lenovo
 */
public class NewMain {

    /**
     * @param args the command line arguments
     */
    public static void main(String[] args) throws IOException {
        // TODO code application logic here
        AlgoIIMFIs algoIIMFIs=new AlgoIIMFIs();
        String DBname="D:\\dataset\\mushroom.txt";
        double min_Sup=.25;
        double minNovlty=.50;
        List<PD_IMFIs> Pi = new ArrayList<>();
        algoIIMFIs.runAlgorithm(DBname, Pi, false, min_Sup, minNovlty);
        algoIIMFIs.printStats();
    }
    
}
