package arm

// https://web.eecs.umich.edu/~prabal/teaching/eecs373-f10/readings/ARMv7-M_ARM.pdf
@Suppress("unused", "UNUSED_PARAMETER")
open class Thumb {
    open fun nop() {
        log { "NOP" }
    }

    open fun mov_r(d: Int, m: Int) {
        log { "MOV ${reg(d)}, ${reg(m)}" }
    }

    open fun mov_i(d: Int, i: Int) {
        log { "MOVS ${reg(d)}, #$i" }
    }

    open fun movw(d: Int, i: Int) {
        log { "MOVW ${reg(d)}, #$i" }
    }

    open fun movt_w(d: Int, i: Int) {
        log { "MOVT.W ${reg(d)}, #$i" }
    }

    open fun sub_i(d: Int, n: Int, i: Int) {
        log { "SUBS ${reg(d)}, ${reg(n)}, #$i" }
    }

    open fun add_i(d: Int, n: Int, i: Int) {
        log { "ADD ${reg(d)}, ${reg(n)}, #$i" }
    }

    open fun bl(pc: Int, target: Int) {
        log { "BL #0x%08X".format(target) }
    }

    open fun push(r: Int) {
        log { "PUSH {%s}".format(getRegList(r).map { reg(it) }.joinToString(", ")) }
    }

    open fun pop(r: Int) {
        log { "POP {%s}".format(getRegList(r).map { reg(it) }.joinToString(", ")) }
    }

    open fun ldr(Rt: Int, Rn: Int, offset: Int) {
        log { "LDR ${reg(Rt)}, [${reg(Rn)}${offset_opt(offset)}]" }
    }

    open fun str(Rt: Int, Rn: Int, offset: Int) {
        log { "STR ${reg(Rt)}, [${reg(Rn)}${offset_opt(offset)}]" }
    }

    open fun bx(m: Int) {
        log { "BX ${reg(m)}" }
    }

    //////////////////////////////////////////////////////////////////////////////////////
    //////////////////////////////////////////////////////////////////////////////////////

    @Ins("1011111100000000") fun _nop(pc:Int) = nop()
    @Ins("01000110:D:mmmm:ddd") fun _mov(pc:Int, d: Int, m: Int, D: Int) = mov_r(d or (D shl 3), m) // MOV (register) T1
    @Ins("0000000000:mmm:ddd") fun _mov(pc:Int, d: Int, m: Int) = mov_r(d, m) // MOV (register) T2
    @Ins("00100:ddd:iiiiiiii") fun _mov_ii(pc:Int, i: Int, d: Int) = mov_i(d, i)

    // 1 1 1 1 0 i 1 0 0 1 0 0 imm4 || 0 imm3 Rd imm8
    // 0 imm3 Rd imm8 || 1 1 1 1 0 i 1 0 0 1 0 0 imm4
    @Ins("0:xxx:xxxx:iiiiiiii::11110:i:100100:iiii")
    fun _movw_t3(pc:Int, imm4:Int, imm1: Int, imm8: Int, rd: Int, imm3: Int) {
        //d = UInt(Rd); setflags = FALSE; imm32 = ZeroExtend(imm4:i:imm3:imm8, 32);
        //if d IN {13,15} then UNPREDICTABLE;
        movw(rd, (imm8 shl 0) or (imm3 shl 8) or (imm1 shl 11) or (imm4 shl 12))

    }

    // 1 1 1 1 0 i 1 0 1 1 0 0 imm4 || 0 imm3 Rd imm8
    // 0 imm3 Rd imm8 || 1 1 1 1 0 i 1 0 1 1 0 0 imm4
    @Ins("0:xxx:xxxx:iiiiiiii::11110:i:101100:iiii")
    fun _movt_w(pc:Int, imm4:Int, imm1: Int, imm8: Int, rd: Int, imm3: Int) {
        movt_w(rd, (imm8 shl 0) or (imm3 shl 8) or (imm1 shl 11) or (imm4 shl 12))
    }

    @Ins("010001110:mmmm:000") // Encoding T1 All versions of the Thumb ISA. BX<c> <Rm>
    fun _bx(pc:Int, m: Int) {
        bx(m)
    }

    @Ins("1011010:M:rrrrrrrr") fun _push(pc:Int, r: Int, M: Int) = push(r or (M shl 14))
    @Ins("1011110:P:rrrrrrrr") fun _pop(pc:Int, r: Int, P: Int) = pop(r or (P shl 15))

    // 1 1 1 1 0 S imm10 || 1 1 J1 1 J2 imm11
    // 1 1 J1 1 J2 imm11 || 1 1 1 1 0 S imm10

    @Ins("11:j:1:J:IIIIIIIIIII::11110:S:iiiiiiiiii")
    fun _bl_i(pc:Int, imm10: Int, S: Int, imm11: Int, J2: Int, J1: Int) {
        // I1 = NOT(J1 EOR S); I2 = NOT(J2 EOR S); imm32 = SignExtend(S:I1:I2:imm10:imm11:'0', 32);
        val I1 = (J1 xor S).inv() and 1
        val I2 = (J2 xor S).inv() and 1
        var out = 0
        out = out or (S shl 23)
        out = out or (I1 shl 22)
        out = out or (I2 shl 21)
        out = out or (imm10 shl 11)
        out = out or (imm11 shl 0)
        val imm = sext(out, 24) + 2
        bl(pc, pc + imm)
    }

    // LDR (immediate) Encoding T1
    // 0 1 1 0 1 imm5 Rn Rt
    @Ins("01101:iiiii:nnn:ttt") fun _ldr_t1(pc:Int, Rt: Int, Rn: Int, imm5: Int) = ldr(Rt, Rn, imm5 * 4)
    @Ins("01100:iiiii:nnn:ttt") fun _str_t1(pc:Int, Rt: Int, Rn: Int, imm5: Int) = str(Rt, Rn, imm5 * 4)

    @Ins("10010:ttt:iiiiiiii") fun _str_sp_t2(pc:Int, i: Int, t: Int) = str(t, SP, zext(i, 8) * 4)

    @Ins("0001111:iii:nnn:ddd") fun _sub_i_t1(pc: Int, d: Int, n: Int, i: Int) = sub_i(d, n, sext(i, 3))
    @Ins("00111:nnn:iiiiiiii") fun _sub_i_t2(pc: Int, i: Int, n: Int) = sub_i(n, n, sext(i, 3))

    //1 0 1 1 0 0 0 0 1 imm7

    @Ins("101100001:iiiiiii") fun _sub_sp_t1(pc: Int, i: Int) = sub_i(SP, SP, i * 4)
    @Ins("101100000:iiiiiii") fun _add_sp_t1(pc: Int, i: Int) = add_i(SP, SP, i * 4)

    // 111110001101 Rn || Rt imm12
    // Rt imm12 || 111110001101 Rn
    @Ins("tttt:iiiiiiiiiiii::111110001101:nnnn")
    fun _ldr_w(pc: Int, n: Int, i: Int, t: Int) {
        println("_ldr_w $pc, $n, $i, $t")
    }

    // 1 1 1 1 1 0 0 0 0 1 0 1 Rn || Rt 0 0 0 0 0 0 imm2 Rm
    // Rt 0 0 0 0 0 0 imm2 Rm || 1 1 1 1 1 0 0 0 0 1 0 1 Rn
    @Ins("tttt:000000:ii:mmmm::111110000101:nnnn")
    fun _ldr_w2(pc: Int, n: Int, m: Int, i: Int, t: Int) {
        println("_ldr_w2 $pc, $n, $m, $i, $t")
    }

    // 1 1 1 1 1 0 0 0 U 1 0 1 1 1 1 1 || Rt imm12
    // Rt imm12 || 11111000 : U : 1011111

    @Ins("tttt:iiiiiiiiiiii:11111000:U:1011111")
    fun _ldr__(pc: Int, U: Int, i: Int, t: Int) {
        println("_ldr__:$pc, $U, $i, $t")
    }

    //1 1 1 1 1 0 0 0 0 1 0 1 Rn || Rt 1 P U W imm8
    //Rt 1 P U W imm8 || 1 1 1 1 1 0 0 0 0 1 0 1 Rn
    @Ins("tttt:1:p:u:w:iiiiiiii::111110000101:nnnn")
    fun _ldr_i_T4(pc: Int, Rn: Int, imm8: Int, wback: Int, add: Int, index:Int, Rt:Int) {
        //if Rn == ’1111’ then SEE LDR (literal);
        //if P == ’1’ && U == ’1’ && W == ’0’ then SEE LDRT;
        //if Rn == ’1101’ && P == ’0’ && U == ’1’ && W == ’1’ && imm8 == ’00000100’ then SEE POP;
        //if P == ’0’ && W == ’0’ then UNDEFINED;
        //t = UInt(Rt); n = UInt(Rn); imm32 = ZeroExtend(imm8, 32);
        //index = (P == ’1’); add = (U == ’1’); wback = (W == ’1’);
        //if (wback && n == t) || (t == 15 && InITBlock() && !LastInITBlock()) then UNPREDICTABLE;
        val simm8 = if (add == 1) +imm8 else -imm8
        val offset = if (wback == 1) simm8 else simm8
        val suffix = if (wback == 1) ".W" else ""

        if (index == 1) {
            log { "LDR$suffix ${reg(Rt)}, [${reg(Rn)}, #$offset]" }
        } else {
            log { "LDR$suffix ${reg(Rt)}, [${reg(Rn)}],#$offset" }
        }

    }

    //////////////////////////////////////////////////////////////////////////////////////
    //////////////////////////////////////////////////////////////////////////////////////

    protected fun offset_opt(v: Int) = if (v == 0) "" else ",#$v"

    protected fun reg(v: Int) = when (v) {
        13 -> "SP"
        14 -> "LR"
        15 -> "PC"
        else -> "R$v"
    }

    protected fun zext(x: Int, b: Int): Int = x

    protected fun sext(x: Int, b: Int): Int {
        val m = 1 shl (b - 1); // mask can be pre-computed if b is fixed
        return ((x and ((1 shl b) - 1)) xor m) - m;
    }

    private fun getRegList(r: Int) = (0 until 16).filter { ((r ushr it) and 1) != 0 }
    private fun getRegList(r: Int, M: Int, P: Int) = getRegList(r or (M shl 14) or (P shl 15))

    open val doLog = true

    inline protected fun log(s: () -> String) {
        if (doLog) _log(s())
    }

    open protected fun _log(s: String) {
        System.out.println(s)
    }

    protected val SP = 13
    protected val LR = 14
    protected val PC = 15
}
