/* *********************************************************************
 * ECE351 
 * Department of Electrical and Computer Engineering 
 * University of Waterloo 
 * Term: Fall 2021 (1219)
 *
 * The base version of this file is the intellectual property of the
 * University of Waterloo. Redistribution is prohibited.
 *
 * By pushing changes to this file I affirm that I am the author of
 * all changes. I affirm that I have complied with the course
 * collaboration policy and have not plagiarized my work. 
 *
 * I understand that redistributing this file might expose me to
 * disciplinary action under UW Policy 71. I understand that Policy 71
 * allows for retroactive modification of my final grade in a course.
 * For example, if I post my solutions to these labs on GitHub after I
 * finish ECE351, and a future student plagiarizes them, then I too
 * could be found guilty of plagiarism. Consequently, my final grade
 * in ECE351 could be retroactively lowered. This might require that I
 * repeat ECE351, which in turn might delay my graduation.
 *
 * https://uwaterloo.ca/secretariat-general-counsel/policies-procedures-guidelines/policy-71
 * 
 * ********************************************************************/

package ece351.f.simgen;

import java.io.PrintWriter;
import java.util.Comparator;
import java.util.Map;
// import java.util.Map.Entry;
import java.util.Set;
import java.util.SortedMap;
import java.util.TreeMap;

import ece351.common.ast.AndExpr;
import ece351.common.ast.AssignmentStatement;
import ece351.common.ast.ConstantExpr;
import ece351.common.ast.EqualExpr;
import ece351.common.ast.Expr;
import ece351.common.ast.NAndExpr;
import ece351.common.ast.NOrExpr;
import ece351.common.ast.NaryAndExpr;
import ece351.common.ast.NaryOrExpr;
import ece351.common.ast.NotExpr;
import ece351.common.ast.OrExpr;
import ece351.common.ast.VarExpr;
import ece351.common.ast.XNOrExpr;
import ece351.common.ast.XOrExpr;
import ece351.common.visitor.PostOrderExprVisitor;
import ece351.f.analysis.DepthCounter;
import ece351.f.analysis.DetermineInputVars;
import ece351.f.ast.FProgram;
import ece351.util.OSDetect;
import ece351.w.ast.WProgram;

public class SimulatorGenerator_x86_64 extends PostOrderExprVisitor {

	/** Name of putchar function. On Mac it is prepended with underscore. */
	private static final String PUTCHAR = (OSDetect.isMac() ? "_" : "") + "putchar";

	/** Windows has different register usage conventions. */
	private static final String ARG_REGISTER = (OSDetect.isWindows() ? "%rcx" : "%rdi");
	
	/** Where the generated code gets written to. */
	private PrintWriter out = new PrintWriter(System.out);
	
	/**
	 * Each AssignmentStatement uses a different subset of the input variables.
	 * So each AssignmentStatement needs its own mapping of input variables to registers.
	 */
	private SortedMap<AssignmentStatement, SortedMap<String, String>> registerAllocation;
	

	/** The current level of indenting. */
	private String indent = "";
	
	/** The current AssignmentStatement. */
	private AssignmentStatement currentAStmt;
	
	/** 
	 * A comparator to sort AssignmentStatements by their output variable,
	 * so that we can store them in a TreeMap (which must be able to sort its elements).
	 */
	private static final Comparator<AssignmentStatement> ASTMT_COMPARATOR = new Comparator<AssignmentStatement>(){

		@Override
		public int compare(AssignmentStatement s1, AssignmentStatement s2) {
			return s1.outputVar.compareTo(s2.outputVar);
		}};


	/** 
	 * Generate x64 assembly to evaluate fprog on wprog.
	 * @param wprog W input to the fprog
	 * @param fprog the F program to be evaluated
	 * @param out where to write the generated x64 assembly code
	 */
	public void generate(final WProgram wprog, final FProgram fprog, final PrintWriter out)
	{
		this.out = (out == null) ? this.out : out;

		// overall header
		genHeader();
		println(""); // lil bit of space
		// allocate storage to remember how registers are allocated
		registerAllocation = new TreeMap<AssignmentStatement, SortedMap<String, String>>(ASTMT_COMPARATOR);

		// allocate registers + generate assembly procedure for each formula
		for (final AssignmentStatement stmt : fprog.formulas)
		{
			// allocate a map for the register 
			allocateRegisters(stmt);
			// genFuncHeader("out_" + stmt.outputVar.identifier);
			//function assumes that header will be loaded onto memory before calling in main.
			generate(stmt); // traverses and goes crazy with the rizz. 
			// genFuncFooter();
			println(""); // lil bit of space
		}
		
		// header for main
		genFuncHeader("main");
		
		// evaluate each formula at each time step
		for (final AssignmentStatement stmt : fprog.formulas)
		{
			printIdentifier(stmt.outputVar.identifier);
			for (int t = 0 ; t < wprog.timeCount() ; t++)
			{
				genx86PutChar(' ');
				// call the procedure to evaluate this formula at this time
				// first load the input variables into the registers
				loadRegisters(wprog, t, stmt); // current statement input vars ready 
				functionCall(stmt);
				outputValue();
				// then output the resulting value
				
			}
			genx86PutChar(';');
			genx86PutChar('\n');
		}
		println("");

		// footer for main
		genFuncFooter();
	}

	/**
	 * Generate x64 assembly to evaluate a formula.
	 * @param stmt the formula to be evaluated
	 */
	public void generate(final AssignmentStatement stmt)
	{
		currentAStmt = stmt;
		
		final String funcName = "out_" + currentAStmt.outputVar;

		// generate header, generate footer
		genFuncHeader(funcName);
		// traverse expr
		traverseAssignmentStatement(stmt);

		// generate footer 
		genFuncFooter();
	}
	
	/** Movq constant value to %rax and pushq it on the stack. */
	@Override
	public Expr visitConstant(final ConstantExpr e) {
		println("movq",e.b ? "$1" : "$0", "%rax");
		println("pushq", "%rax");
		return e;
	}

	/** Pushq a variable onto the stack from its allocated register. */
	@Override
	public Expr visitVar(final VarExpr e) {
		// doesnt matter if there is duplicate moving 
		String reg_name = registerAllocation.get(currentAStmt).get(e.identifier); // search via string now 
		// println("movq" , reg_name, "%rax");
		println("pushq", reg_name);
		println("// visit VAR return for " + e);
		return e;
	}

	/** Expect operand in %rax. What operator to use? Think bitwise. */
	@Override
	public Expr visitNot(final NotExpr e) {
		// traverseExpr(e.expr); // looks like it traverses kinda automatically
		println("popq", "%rax"); // get result from traversal
		println("notq" , "%rax"); // flip rax since that has the result
		println("andq", "$1", "%rax"); // only take the first bit
		println("pushq", "%rax"); // push the result for next operators
		println("// visit NOT return for " + e);
		return e;
	}

	/** Expect operands in %rax and %rbx. */
	@Override
	public Expr visitAnd(final AndExpr e) {
		// traverseExpr(e.left);
		// traverseExpr(e.right);
		println("popq", "%rax");
		println("popq", "%rbx");
		println("andq", "%rbx", "%rax");
		println("pushq", "%rax");
		println("// visit AND return with operands " + e.left + " AND " + e.right);
		return e;
	}

	/** Expect operands in %rax and %rbx. */
	@Override
	public Expr visitOr(final OrExpr e) {
// TODO: short code snippet
		// traverseExpr(e.left);
		// traverseExpr(e.right);
		println("popq", "%rax");
		println("popq", "%rbx");
		println("orq", "%rbx", "%rax");
		println("pushq", "%rax");
		println("// visit AND return with operands " + e.left + " OR " + e.right);
		return e;
	}

	@Override public Expr visitNaryAnd(final NaryAndExpr e) {
		throw new UnsupportedOperationException(); 
	}
	@Override public Expr visitNaryOr(final NaryOrExpr e) { 
		throw new UnsupportedOperationException(); 
	}
	@Override public Expr visitNOr(final NOrExpr e) { throw new UnsupportedOperationException(); }
	@Override public Expr visitXOr(final XOrExpr e) { throw new UnsupportedOperationException(); }
	@Override public Expr visitXNOr(final XNOrExpr e) { throw new UnsupportedOperationException(); }
	@Override public Expr visitNAnd(final NAndExpr e) { throw new UnsupportedOperationException(); }
	@Override public Expr visitEqual(final EqualExpr e) { throw new UnsupportedOperationException(); }

	private void loadRegisters(final WProgram wprog, final int t, final AssignmentStatement stmt) {
		final Map<String, String> mvars_regs = registerAllocation.get(stmt);
		
			// for every input register 
		for( Map.Entry<String, String> entry : mvars_regs.entrySet()) {
			final int value = wprog.valueAtTime(entry.getKey(), t) ? 1 : 0;
			load_reg(value, entry.getValue()); // holds the r# reg nums
		}
	}

	private void allocateRegisters(final AssignmentStatement stmt)
	{
		final Set<String> inputs = DetermineInputVars.inputVars(stmt);
		if (inputs.size() > 8)
		{
			throw new UnsupportedOperationException("can't process formula with more than 8 input variables");
		}
		int reg_offset = 8;
		SortedMap<String, String> regMap = new TreeMap<>();
		
		// map them 
		// for each var Expr you find add the r# in string form... 
	
		for(String s: inputs) {
			regMap.put(s, "%r" + reg_offset);
			reg_offset++;
		}
		registerAllocation.put(stmt, regMap);
	}
	
	private void genHeader()
	{
		indent();
		indent();
		println(".text");
	}
	
	private void genFuncHeader(final String name)
	{
		println(".globl\t" + name);
		if (OSDetect.isNix()) {
			// Windows and Mac do not like these type declarations
			println(".type\t" + name + ", @function");
		}
		outdent();
		outdent();
		println(name + ":");
		indent();
		indent();
		println("pushq", "%rbp");
		println("movq", "%rsp", "%rbp");
	}
	
	private void genFuncFooter()
	{
		println("popq", "%rax");
		println("leave");
		println("ret");
	}
	
	private void outputValue() {
		// movq value to output into ARG_REGISTER
		// TODO: short code snippet
		println("movq", "%rax" , ARG_REGISTER); // retrieve the function output from stacc
		println("add", "$48", ARG_REGISTER); // char for 0 is ASCII 48, adding the offset necessary
		println("call", PUTCHAR);
	}

	private void functionCall(final AssignmentStatement stmt) {
		println("call", "out_" + stmt.outputVar);
	}

	private void printIdentifier(final String identifier) {
		for (int i = 0 ; i < identifier.length() ; i++)
		{
			char ch = identifier.charAt(i);
			genx86PutChar(ch);
		}	
		genx86PutChar(':');
	}

	private void load_reg(final int value, String reg_name ) {
		println("movq", "$" + value, reg_name);
	}

	private void genx86PutChar(final char ch) {
		println("movq", "$" + (int)ch, ARG_REGISTER);
		println("call", PUTCHAR);
	}
	
	private void println(final String s) {
		out.print(indent);
		out.println(s);
	}
	
	public void println(final String command, final String arg1)
	{
		println(command + "\t" + arg1);
	}
	
	public void println(final String command, final String arg1, final String arg2)
	{
		println(command + "\t" + arg1 + ", " + arg2);
	}
	
	private void indent() {
		indent = indent + "    ";
	}
	
	private void outdent() {
		indent = indent.substring(0, indent.length() - 4);
	}

}
