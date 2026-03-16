package template;

import br.com.davidbuzatto.jsge.core.engine.EngineFrame;
import br.com.davidbuzatto.jsge.image.Image;
import br.com.davidbuzatto.jsge.imgui.GuiButton;
import br.com.davidbuzatto.jsge.imgui.GuiInputDialog;
import java.awt.Color;
import java.util.*;

/**
 * @author jdavicunha
 */
public class Main extends EngineFrame {

    private static final int TAMANHO = 3;
    private Peca[][] grade;
    private double tamanhoPeca;
    private Image imagemPeca;

    private GuiButton botaoShuffle;
    private GuiButton botaoResolver;
    private GuiButton botaoDefinirLimite;

    private GuiInputDialog entradaLimite;
    
    private int limiteProfundidade = 500;
    
    private int contadorMovimentos;
    private boolean simulando;
    private int framesSimulacao = 0;

    private List<int[]> rotaSolucao;
    private int passoAtual;
    private final String ESTADO_OBJETIVO = "012345678";
    private HashSet<String> visitados; 

    public Main() {
        super(1000, 600, "Puzzle - JoaoDaviCunha", 60, true);
    }

    @Override
    public void create() {
        useAsDependencyForIMGUI();
        
        grade = new Peca[TAMANHO][TAMANHO];
        tamanhoPeca = 600 / TAMANHO; 
        imagemPeca = loadImage("resources/images/goku.png");
        imagemPeca.resize(600, 600);

        botaoResolver = new GuiButton(25, 50, 150, 40, "BACKTRACK");
        botaoDefinirLimite = new GuiButton(25, 100, 150, 40, "SET LIMIT");
        botaoShuffle = new GuiButton(825, 50, 150, 40, "SHUFFLE");

        entradaLimite = new GuiInputDialog("CONFIGURAR LIMITE", "Digite a profundidade máxima (inteiro):", true);

        botaoResolver.setBackgroundColor(Color.DARK_GRAY);
        botaoShuffle.setBackgroundColor(Color.DARK_GRAY);
        botaoDefinirLimite.setBackgroundColor(Color.BLUE);
        
        botaoResolver.setTextColor(WHITE);
        botaoDefinirLimite.setTextColor(WHITE);
        botaoShuffle.setTextColor(WHITE);
        
        rotaSolucao = new ArrayList<>();
        visitados = new HashSet<>();
        inicializarGrade();
    }

    @Override
    public void update(double delta) {

        botaoResolver.update(delta);
        botaoShuffle.update(delta);
        botaoDefinirLimite.update(delta);
        entradaLimite.update(delta);

        if (botaoDefinirLimite.isMousePressed()) {
            entradaLimite.show();
        }

        if (entradaLimite.isOkButtonPressed() || entradaLimite.isEnterKeyPressed()) {
            String valorDigitado = entradaLimite.getValue();
            if (checarInteiro(valorDigitado)) {
                limiteProfundidade = Integer.parseInt(valorDigitado);
                entradaLimite.hide();
            } else {
                System.out.println("Erro");
            }
        }
        
        if (entradaLimite.isCancelButtonPressed() || entradaLimite.isCloseButtonPressed()) {
            entradaLimite.hide();
        }

        if (botaoResolver.isMousePressed() && !simulando) {
            iniciarBacktracking();
        }

        if (botaoShuffle.isMousePressed()) {
            simulando = false;
            embaralhar();
        }

        if (simulando && passoAtual < rotaSolucao.size()) {
            framesSimulacao++;
            if (framesSimulacao >= 2) { 
                int[] p = rotaSolucao.get(passoAtual);
                realizarMovimentoVazio(p[0], p[1]);
                passoAtual++;
                framesSimulacao = 0;
                if (passoAtual == rotaSolucao.size()) simulando = false;
            }
        }

        if (isMouseButtonPressed(MOUSE_BUTTON_LEFT) && !simulando) {
            int mx = getMouseX();
            int my = getMouseY();
            if (mx >= 200 && mx <= 800) {
                for (int i=0; i<3; i++) for (int j=0; j<3; j++) 
                    if (grade[i][j] != null && grade[i][j].intercepta(mx, my)) moverPeca(i, j);
            }
        }
    }

    @Override
    public void draw() {
        clearBackground(WHITE);
        
        for (int i=0; i<3; i++) 
            for (int j=0; j<3; j++) 
                if (grade[i][j] != null) grade[i][j].desenhar(this, 3);
        
        fillRectangle(0, 0, 200, 600, LIGHTGRAY);
        fillRectangle(800, 0, 200, 600, LIGHTGRAY);

        botaoResolver.draw();
        botaoDefinirLimite.draw();
        botaoShuffle.draw();
        entradaLimite.draw();

        drawText("Limite Atual:", 25, 160, 18, BLACK);
        drawText(String.valueOf(limiteProfundidade), 25, 185, 22, BLACK);

        drawText("Movimentos:", 825, 150, 20, BLACK);
        String texto = simulando ? (passoAtual + " / " + rotaSolucao.size()) : String.valueOf(contadorMovimentos);
        drawText(texto, 825, 180, 30, BLACK);
    }

    private boolean checarInteiro(String str) {
        try {
            int val = Integer.parseInt(str);
            return val >= 0;
        } catch (NumberFormatException e) {
            return false;
        }
    }

    private void inicializarGrade() {
        for (int i = 0; i < TAMANHO; i++) {
            for (int j = 0; j < TAMANHO; j++) {
                grade[i][j] = new Peca(200 + (j * tamanhoPeca), i * tamanhoPeca, tamanhoPeca, i * TAMANHO + j, imagemPeca);
            }
        }
        grade[TAMANHO - 1][TAMANHO - 1] = null;
        contadorMovimentos = 0;
        simulando = false;
    }

    private void iniciarBacktracking() {
        rotaSolucao.clear();
        visitados.clear();
        passoAtual = 0;
        contadorMovimentos = 0;
        String inicial = lerEstadoAtual();
        int lv = -1, cv = -1;
        for(int i=0; i<TAMANHO; i++) for(int j=0; j<TAMANHO; j++) if(grade[i][j] == null) { lv=i; cv=j; }

        if (executarBacktracking(inicial, lv, cv, 0)) { 
            simulando = true;
        } else {
            System.out.println("Solução não encontrada no limite de " + limiteProfundidade);
        }
    }

    private boolean executarBacktracking(String estado, int lv, int cv, int profundidade) {
        if (profundidade > limiteProfundidade) return false;
        if (estado.equals(ESTADO_OBJETIVO)) return true;
        if (visitados.contains(estado)) return false;
        visitados.add(estado);

        int[] vL = {-1, 0, 1, 0}, vC = {0, 1, 0, -1};
        for (int i = 0; i < 4; i++) {
            int nL = lv + vL[i], nC = cv + vC[i];
            if (nL >= 0 && nL < TAMANHO && nC >= 0 && nC < TAMANHO) {
                String proximo = trocar(estado, lv, cv, nL, nC);
                if (executarBacktracking(proximo, nL, nC, profundidade + 1)) {
                    rotaSolucao.add(0, new int[]{nL, nC}); 
                    return true;
                }
            }
        }
        return false;
    }

    private String lerEstadoAtual() {
        char[] c = new char[9];
        for (int i=0; i<3; i++) for (int j=0; j<3; j++) 
            c[i*3+j] = (grade[i][j] == null) ? '8' : (char)(grade[i][j].getValor() + '0');
        return new String(c);
    }

    private String trocar(String s, int l1, int c1, int l2, int c2) {
        char[] a = s.toCharArray();
        int p1 = l1*3+c1, p2 = l2*3+c2;
        char t = a[p1]; a[p1] = a[p2]; a[p2] = t;
        return new String(a);
    }

    private void realizarMovimentoVazio(int nL, int nC) {
        int lv=-1, cv=-1;
        for(int i=0; i<3; i++) for(int j=0; j<3; j++) if(grade[i][j]==null){lv=i;cv=j;}
        grade[lv][cv] = grade[nL][nC]; grade[nL][nC] = null;
        contadorMovimentos++;
        recalcularPosicoes();
    }

    private void moverPeca(int lin, int col) {
        int[] vL={-1,0,1,0}, vC={0,1,0,-1};
        for (int i=0; i<4; i++) {
            int nL=lin+vL[i], nC=col+vC[i];
            if (nL>=0 && nL<3 && nC>=0 && nC<3 && grade[nL][nC]==null) {
                grade[nL][nC]=grade[lin][col]; grade[lin][col]=null;
                contadorMovimentos++; recalcularPosicoes(); break;
            }
        }
    }

    private void embaralhar() {
        inicializarGrade();
        for (int i = 0; i < 100; i++) {
            int lv=-1, cv=-1;
            for(int r=0; r<3; r++) for(int c=0; c<3; c++) if(grade[r][c]==null){lv=r;cv=c;}
            int d = (int)(Math.random()*4);
            int[] vL={-1,0,1,0}, vC={0,1,0,-1};
            int nL=lv+vL[d], nC=cv+vC[d];
            if(nL>=0 && nL<3 && nC>=0 && nC<3) {
                grade[lv][cv] = grade[nL][nC]; grade[nL][nC] = null;
            }
        }
        contadorMovimentos = 0;
        recalcularPosicoes();
    }

    private void recalcularPosicoes() {
        for (int i=0; i<3; i++) for (int j=0; j<3; j++) 
            if (grade[i][j] != null) grade[i][j].setPos(200 + (j*(600.0/3)), i*(600.0/3));
    }

    public static void main(String[] args) { new Main(); }
}