/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package ru.mipt.archtoys.yapm.bt;

import java.util.Iterator;
import java.util.LinkedList;
import ru.mipt.archtoys.star.asm.Instruction;
import ru.mipt.archtoys.star.asm.Instruction.OperAddr;
import ru.mipt.archtoys.star.asm.Instruction.OperFloat;
import ru.mipt.archtoys.star.asm.Instruction.OperInteger;
import ru.mipt.archtoys.yapm.bt.Op.OpConstFloat;
import ru.mipt.archtoys.yapm.bt.Op.OpConstInt;
import ru.mipt.archtoys.yapm.bt.Op.OpMem;
import ru.mipt.archtoys.yapm.bt.Op.OpReg;

/**
 *
 * @author danisimo
 */
public class Lowering {

    private Ir ir;
    private int tosAddr;
    private int regNum;

    public Lowering(Ir i_r) {
        ir = i_r;
        tosAddr = 0;
        regNum = 0;
    }

    public LinkedList<Operation> runLowering() {
        Iterator<Instruction> iter;
        iter = ir.starAsm.iterator();
        while (iter.hasNext()) {
            Instruction instr = iter.next();
            switch (instr.defs) {
            case LDCI:
            case LDCD:
                lowirLdConst(instr);
                break;
            case LDI:
            case LDD:
                lowirLdShift(instr);
                break;
            case LDSI:
            case LDSD:
                lowirLdDyn(instr);
                break;
            case STI:
            case STD:
                lowirStDyn(instr);
                break;
            case ALLOC:
                lowirAlloc(instr);
                break;
            case MRI:
            case MRD:
                lowirMa(instr);
                break;
            case SCR:
                lowirScr(instr);
                break;
            case LDA:
                lowirLdAddr(instr);
                break;
            case INDEX:
                lowirIndex(instr);
                break;
            case ADDI:
            case ADDD:
            case SUBI:
            case SUBD:
            case MULI:
            case MULD:
            case DIVI:
            case DIVD:
            case REM:
                lowirArith(instr);
                break;
            case CHSI:
            case CHSD:
                lowirChs(instr);
            case FI2D:
            case FD2I:
                lowirConv(instr);
                break;
            case MS:
                lowirMs(instr);
                break;
            case CALL:
                lowirCall(instr);
                break;
            }
        }
        return ir.seq;
    }

    private void lowirLdConst(Instruction instr) {
        /*
         * Construct load of constant to register.
         */
        Operation oper = new Operation("ldc");
        int tosIncr = getStackOperSize(instr);
        switch (instr.defs.getOperType()) {
        case INT:
            int valueInt = ((OperInteger) instr.oper).value;
            oper.args.add(new OpConstInt(valueInt));
            break;
        case FLOAT:
            float valueFloat = ((OperFloat) instr.oper).value;
            oper.args.add(new OpConstFloat(valueFloat));
            break;
        default:
            assert false;
        }
        int reg = nextReg();
        oper.res.add(new OpReg(reg));
        ir.seq.add(oper);
        /*
         * Construct store from register to 'tos' address
         */
        oper = new Operation("st");
        oper.args.add(new OpReg(reg));
        oper.res.add(new OpMem(tosAddr));
        ir.seq.add(oper);
        /*
         * Correct tos address
         */
        tosAddr += tosIncr;
    }

    private void lowirLdShift(Instruction instr) {
        /*
         * Construct load from address 'shift' to register
         */
        Operation oper = new Operation("ld");
        int tosIncr = getStackOperSize(instr);
        int shift = ((OperAddr) instr.oper).value;
        int reg = nextReg();
        oper.args.add(new OpMem(shift));
        oper.res.add(new OpReg(reg));
        ir.seq.add(oper);
        /*
         * Construct store from register to 'tos' address
         */
        oper = new Operation("st");
        oper.args.add(new OpReg(reg));
        oper.res.add(new OpMem(tosAddr));
        ir.seq.add(oper);
        /*
         * Correct tos address
         */
        tosAddr += tosIncr;
    }

    private void lowirLdDyn(Instruction instr) {
        /*
         * Construct load from address 'tos' to register
         */
        Operation oper = new Operation("ld");
        int tosIncr = getStackOperSize(instr);
        int reg = nextReg();
        oper.args.add(new OpMem(tosAddr - 2)); // Address is 2byte wide
        oper.res.add(new OpReg(reg));
        ir.seq.add(oper);
        /*
         * Construct load of 0 to base register
         */
        int reg1 = nextReg();
        oper = new Operation("ldc");
        oper.args.add(new OpConstInt(0));
        oper.args.add(new OpReg(reg1));
        ir.seq.add(oper);
        /*
         * Construct ld from address in index register to another register
         */
        oper = new Operation("lda");
        int reg2 = nextReg();
        oper.args.add(new OpReg(reg1));
        oper.args.add(new OpReg(reg));
        oper.res.add(new OpReg(reg2));
        ir.seq.add(oper);
        /*
         * Construct st from register to 'tos' address
         */
        oper = new Operation("st");
        oper.args.add(new OpReg(reg2));
        oper.res.add(new OpMem(tosAddr - 2));
        ir.seq.add(oper);
        /*
         * Correct tos address
         */
        tosIncr -= 2; // Address is removed from tos
        tosAddr += tosIncr;
    }

    private void lowirStDyn(Instruction instr) {
        /*
         * Construct load address from address 'tos' to register
         */
        Operation oper = new Operation("ld");
        int reg = nextReg();
        oper.args.add(new OpMem(tosAddr - 2)); // Address is 2byte wide
        oper.res.add(new OpReg(reg));
        ir.seq.add(oper);
        /*
         * Construct load value from address 'tos-1' to register
         */
        int tosIncr = getStackOperSize(instr);
        oper = new Operation("ld");
        int reg1 = nextReg();
        oper.args.add(new OpMem(tosAddr - 2 - tosIncr)); // Address is 2byte wide
        // Value is 1-2byte
        oper.res.add(new OpReg(reg1));
        ir.seq.add(oper);
        /*
         * Construct load of 0 to base register
         */
        int reg2 = nextReg();
        oper = new Operation("ldc");
        oper.args.add(new OpConstInt(0));
        oper.args.add(new OpReg(reg1));
        ir.seq.add(oper);
        /*
         * Construct st from register to address in index
         */
        oper = new Operation("sta");
        oper.args.add(new OpReg(reg2));
        oper.args.add(new OpReg(reg1));
        oper.args.add(new OpReg(reg));
        ir.seq.add(oper);
        /*
         * Correct tos address
         */
        tosIncr = -tosIncr - 2; // Address is removed from tos
        tosAddr += tosIncr;
    }

    private void lowirAlloc(Instruction instr) {
        int tosIncr = ((OperInteger) instr.oper).value;
        tosAddr += tosIncr;
    }

    private void lowirMa(Instruction instr) {
        throw new UnsupportedOperationException("Not yet implemented");
    }

    private void lowirScr(Instruction instr) {
        throw new UnsupportedOperationException("Not yet implemented");
    }

    private void lowirLdAddr(Instruction instr) {
        /*
         * Construct load of address constant to register.
         */
        Operation oper = new Operation("ldc");
        int tosIncr = 2;
        int addr = ((OperAddr) instr.oper).value;
        int reg = nextReg();
        oper.res.add(new OpConstInt(addr));
        oper.res.add(new OpReg(reg));
        ir.seq.add(oper);
        /*
         * Construct store from register to 'tos' address
         */
        oper = new Operation("st");
        oper.args.add(new OpReg(reg));
        oper.res.add(new OpMem(tosAddr));
        ir.seq.add(oper);
        /*
         * Correct tos address
         */
        tosAddr += tosIncr;
    }

    private void lowirIndex(Instruction instr) {
        throw new UnsupportedOperationException("Not yet implemented");
    }

    private void lowirArith(Instruction instr) {
        String arithName = instr.defs.toString().substring(0, 3);
        int tosIncr = getStackOperSize(instr);
        /*
         * Construct load from 'tos-1' to register
         */
        Operation oper = new Operation("ld");
        int reg = nextReg();
        oper.args.add(new OpMem(tosAddr - tosIncr * 2));
        oper.res.add(new OpReg(reg));
        ir.seq.add(oper);
        /*
         * Construct load from 'tos' to register
         */
        oper = new Operation("ld");
        int reg1 = nextReg();
        oper.args.add(new OpMem(tosAddr - tosIncr));
        oper.res.add(new OpReg(reg1));
        ir.seq.add(oper);
        /*
         * Construct arithm operation
         */
        oper = new Operation(arithName);
        int reg2 = nextReg();
        oper.args.add(new OpReg(reg));
        oper.args.add(new OpReg(reg1));
        oper.res.add(new OpReg(reg2));
        ir.seq.add(oper);
        /*
         * Store result to memory
         */
        oper = new Operation("st");
        oper.args.add(new OpMem(tosAddr - tosIncr * 2));
        oper.args.add(new OpReg(reg2));
        ir.seq.add(oper);
        /*
         * Correct tos address
         */
        tosAddr -= tosIncr;
    }

    private void lowirChs(Instruction instr) {
        throw new UnsupportedOperationException("Not yet implemented");
    }

    private void lowirConv(Instruction instr) {
        throw new UnsupportedOperationException("Not yet implemented");
    }

    private void lowirMs(Instruction instr) {
        throw new UnsupportedOperationException("Not yet implemented");
    }

    private void lowirCall(Instruction instr) {
        throw new UnsupportedOperationException("Not yet implemented");
    }

    private int nextReg() {
        return regNum++;
    }

    private int getStackOperSize(Instruction instr) {
        switch (instr.defs) {
        case LDCI:
        case LDI:            
        case LDSI:
        case STI:
        case MRI:
        case ADDI:
        case SUBI:
        case MULI:
        case DIVI:
        case CHSI:
            return 1;
        case LDCD:
        case LDD:            
        case LDSD:
        case STD:
        case MRD:
        case ADDD:
        case SUBD:
        case MULD:
        case DIVD:
        case CHSD:
            return 2;
        default:
            assert false;
        }
        assert false;
        return 0;
    }
}
