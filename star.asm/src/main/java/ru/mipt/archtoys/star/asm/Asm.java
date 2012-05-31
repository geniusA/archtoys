package ru.mipt.archtoys.star.asm;

import java.util.Iterator;
import java.util.LinkedList;

/**
 * Hello world!
 *
 */
public class Asm 
{
    private static Reader reader;
    
    public static void main( String[] args )
    {
        reader = new Reader("input.star");
        LinkedList<Instruction> list = reader.readAll();
        Iterator<Instruction> iter = list.descendingIterator();
        while ( iter.hasNext()){
            System.out.println( iter.next().defs);
        }
    }
}
