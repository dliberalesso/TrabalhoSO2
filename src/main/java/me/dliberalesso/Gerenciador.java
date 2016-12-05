package me.dliberalesso;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.*;
import java.util.concurrent.*;

import static me.dliberalesso.Gerenciador.*;

public class Gerenciador implements Runnable{
    private ServerSocket serverSocket;
    private ExecutorService pool;
    private int poolSize;

    // Fila + Processos
    private String CABECALHO = " PID | NOME | TAMANHO |  CRIACAO | EXECUCAO |   STATUS   | HOST";
    private List<Processo> executando = Collections.synchronizedList(new ArrayList<Processo>());
    private List<Processo> executado = Collections.synchronizedList(new ArrayList<Processo>());
    private ArrayBlockingQueue<Processo> fila = new ArrayBlockingQueue<>(100);
    private int pid = 1;

    // Memoria
    public static int paginasSecundariaLivres = 32;
    public static String[][] memoriaSecundaria = new String[32][8];
    public static int[][] tpv = new int[32][3];

    public static int paginasPrincipalLivres = 16;
    public static String[][] memoriaPrincipal = new String[16][8];
    public static int[][] tpp = new int[16][2];

    // Quantum
    private int quantum = ThreadLocalRandom.current().nextInt(5, 11);

    // Logger
    public static final Logger logger = LogManager.getLogger(Gerenciador.class);

    public Gerenciador(int porta, int poolSize) throws IOException {
        this.serverSocket = new ServerSocket(porta);
        this.pool = Executors.newFixedThreadPool(poolSize);
        this.poolSize = poolSize;

        // Inicializa memoria secundaria
        for (int i = 0; i < 32; i++) {
            String[] pagina = memoriaSecundaria[i];
            for (int j = 0; j < 8; j++) {
                pagina[j] = "-";
            }
        }

        // Inicializa TPV
        for (int i = 0; i < 32; i++) {
            tpv[i][0] = -1;
        }

        // Inicializa memoria principal
        for (int i = 0; i < 16; i++) {
            String[] pagina = memoriaPrincipal[i];
            for (int j = 0; j < 8; j++) {
                pagina[j] = "-";
            }
        }

        // Inicializa TPP
        for (int i = 0; i < 16; i++) {
            tpp[i][0] = -1;
        }

        // Cabecalho do log é um pouco diferente do PS
        logger.trace("Processos listados na ordem em que finalizados.\n");
        logger.trace(" PID | NOME | TAMANHO |  CRIACAO | EXECUCAO | HOST |   FIM");
    }

    @Override
    public void run() {
        System.out.println("O quantum é de " + quantum + " segundos.");
        try {
            for (int i = 1; i <= poolSize; i++) {
                pool.execute(new ServerHandler(serverSocket.accept(), executando, executado, fila, quantum, i));
            }
            entradas();
        } catch (IOException e) {
            pool.shutdown();
        }
    }

    private void entradas() {
        Scanner scanner = new Scanner(System.in);

        while (true) {
            StringTokenizer st = new StringTokenizer(scanner.nextLine().toLowerCase());
            String comando = st.nextToken();

            if (comando.equals("ps")) {
                ps(st);
            } else if (comando.equals("create")) {
                create(st);
            } else if (comando.equals("now")) {
                System.out.println(Processo.agora());
            } else if (comando.equals("exit")) {
                exit();
                break;
            } else if (comando.equals("mem")) {
                mem(st);
            }
            else {
                System.out.println("Comando invalido!");
            }
        }
    }

    private void ps(StringTokenizer st) {
        if (!st.hasMoreTokens()) {
            System.out.println(CABECALHO);
            for (Processo p : executando) {
                System.out.println(p);
            }
            for (Processo p : fila) {
                System.out.println(p);
            }
        } else {
            int pid = Integer.parseInt(st.nextToken());
            Processo processo = null;

            for (Processo p : executando) {
                if (p.getPid() == pid) {
                    processo = p;
                    break;
                }
            }

            if (processo == null) {
                for (Processo p : executado) {
                    if (p.getPid() == pid) {
                        processo = p;
                        break;
                    }
                }
            }

            if (processo == null) {
                for (Processo p : fila) {
                    if (p.getPid() == pid) {
                        processo = p;
                        break;
                    }
                }
            }

            System.out.println(CABECALHO);
            System.out.println(processo);
            System.out.println("  NPL |  NPV");
            int i = 0;
            for(Integer endereco: processo.getMemoria()) {
                String logica = String.format("%5s", Integer.toBinaryString(i)).replace(' ', '0');
                String secundaria = String.format("%5s", Integer.toBinaryString(endereco)).replace(' ', '0');
                System.out.println(logica + " | " + secundaria);
                i++;
            }
        }
    }

    private void create(StringTokenizer st) {
        if (st.countTokens() != 3) {
            System.out.println("Comando invalido!");
        } else {
            String nome = st.nextToken();
            int tempo = Integer.parseInt(st.nextToken());
            int tamanho = Integer.parseInt(st.nextToken());
            int numeroPaginas = (tamanho + 7) / 8;

            if (numeroPaginas > paginasSecundariaLivres) {
                System.out.println("Não ha memoria secundaria suficiente para este processo.");
            } else {
                Processo processo = new Processo(pid++, nome, tempo, tamanho);
                alocaSecundaria(processo);
                fila.add(processo);
                System.out.println("Criado. PID: " + processo.getPid());
            }
        }
    }

    private void alocaSecundaria(Processo processo) {
        int tamanho = processo.getTamanho();
        int numeroPaginas = (tamanho + 7) / 8;
        String nome = processo.getNome();
        ArrayList<Integer> tabelaProcesso = processo.getMemoria();

        for (int i = 0; i < 32; i++) {
            if (numeroPaginas > 0) {
                if (tpv[i][2] == 0) {
                    tpv[i][2] = 1;
                    tabelaProcesso.add(new Integer(i));
                    String[] pagina = memoriaSecundaria[i];

                    for(int j = 0; j < 8; j++) {
                        if (tamanho > 0) {
                            pagina[j] = nome;
                            tamanho--;
                        } else {
                            pagina[j] = "-";
                        }
                    }
                    numeroPaginas--;
                    paginasSecundariaLivres--;
                }
            }
        }
    }

    private void exit() {
        pool.shutdown(); // Evita que novas tarefas sejam escalonadas
        fila.clear();
        try {
            // Aguarda 1 segundo para que tarefas terminem
            if (!pool.awaitTermination(1, TimeUnit.SECONDS)) {
                pool.shutdownNow(); // Tenta cancelar tarefas que ainda nao terminaram
            }
        } catch (InterruptedException ie) {
            pool.shutdownNow();
            Thread.currentThread().interrupt();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
    
    private void mem(StringTokenizer st) {
        if (!st.hasMoreTokens()) {
            System.out.println("Tamanho em Bytes:\n"
                    + "Memória Principal = " + 128 + " B\n"
                    + "Memória Secundaria = " + 256 + " B");
        } else if (st.countTokens() == 1) {
            String str = st.nextToken();

            switch (str) {
                case ("p"):
                    for (int i = 0; i < 16; i++) {
                        String[] pagina = memoriaPrincipal[i];
                        String binario = String.format("%5s", Integer.toBinaryString(i)).replace(' ', '0');
                        System.out.print(binario + " | ");
                        for (int j = 0; j < 8; j++) {
                            System.out.print(pagina[j]);
                            if (j == 7) {
                                System.out.print("\n");
                            } else {
                                System.out.print(" | ");
                            }
                        }
                    }
                    break;
                case ("v"):
                    for (int i = 0; i < 32; i++) {
                        String[] pagina = memoriaSecundaria[i];
                        String binario = String.format("%5s", Integer.toBinaryString(i)).replace(' ', '0');
                        System.out.print(binario + " | ");
                        for (int j = 0; j < 8; j++) {
                            System.out.print(pagina[j]);
                            if (j == 7) {
                                System.out.print("\n");
                            } else {
                                System.out.print(" | ");
                            }
                        }
                    }
                    break;
                case ("t"):
                    // TPV
                    System.out.println(" NPV  |  NPP  |  BP  |  BV");
                    for (int i = 0; i < 32; i++) {
                        int[] linha = tpv[i];
                        String npv = String.format("%5s", Integer.toBinaryString(i)).replace(' ', '0');
                        String npp;
                        if (linha[0] == -1) {
                            npp = String.format("%4s", -1);
                        } else {
                            npp = String.format("%4s", Integer.toBinaryString(linha[0])).replace(' ', '0');
                        }
                        String bp = String.format("%d", linha[1]);
                        String bv = String.format("%d", linha[2]);
                        System.out.println(npv + " |  " + npp + " |  "  + bp + "   |  " + bv);
                    }

                    // TPP
                    System.out.println(" NPV  |  NPP  |  BV");
                    for (int i = 0; i < 16; i++) {
                        int[] linha = tpp[i];
                        String npv;
                        if (linha[0] == -1) {
                            npv = String.format("%5s", -1);
                        } else {
                            npv = String.format("%5s", Integer.toBinaryString(linha[0])).replace(' ', '0');
                        }
                        String npp = String.format("%4s", Integer.toBinaryString(i)).replace(' ', '0');
                        String bv = String.format("%d", linha[1]);
                        System.out.println(npv + " |  " + npp + " |  "  + bv);
                    }
                    break;
                default:
                    System.out.println("Comando invalido!");
            }
        } else {
            System.out.println("Comando invalido!");
        }
    }
}

class ServerHandler implements Runnable {
    private Socket socket;
    private List<Processo> executando;
    private List<Processo> executado;
    private ArrayBlockingQueue<Processo> fila;
    private int quantum;
    private int hostID;

    private ObjectOutputStream outputStream;
    private ObjectInputStream inputStream;

    ServerHandler(Socket socket, List<Processo> executando, List<Processo> executado, ArrayBlockingQueue<Processo>
            fila, int quantum, int
            hostID) {
        this.socket = socket;
        this.executando = executando;
        this.executado = executado;
        this.fila = fila;
        this.quantum = quantum;
        this.hostID = hostID;

        try {
            this.outputStream = new ObjectOutputStream(socket.getOutputStream());
            this.inputStream = new ObjectInputStream(socket.getInputStream());
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    @Override
    public void run() {
        try {
            // envia o ID do host e aguarda uma confirmação
            outputStream.writeInt(hostID);
            outputStream.flush();

            // se o ID estiver correto, inicia a conversa
            if (inputStream.readInt() == hostID) {
                System.out.println("Host " + hostID + " conectado.");
                outputStream.writeInt(quantum);
                outputStream.flush();

                while (!Thread.interrupted()) {
                    Processo processo = fila.take();

                    if (processo.getRetorno() == 2) {
                        ArrayList<Integer> tabelaProcesso = processo.getMemoria();
                        int tamanhoTabela = tabelaProcesso.size();
                        int paginasNecessarias = (tamanhoTabela + 1) / 2;

                        if (paginasNecessarias > paginasPrincipalLivres) {
                            System.out.println("Memoria insuficiente para o processo " + processo.getPid());
                            processo.setEstado("Abortado");
                            executado.add(processo);
                            continue;
                        } else {
                            ArrayList<Integer> npvs = new ArrayList();
                            for (int i = 0; i < tamanhoTabela; i += 2) {
                                npvs.add(tabelaProcesso.get(i));
                            }

                            for (Integer npv: npvs) {
                                for (int npp = 0; npp < 16; npp++) {
                                    if (tpp[npp][1] == 0) {
                                        tpp[npp][1] = 1;
                                        tpp[npp][0] = npv;
                                        tpv[npv][0] = npp;
                                        tpv[npv][1] = 1;
                                        memoriaPrincipal[npp] = memoriaSecundaria[npv];
                                        paginasPrincipalLivres--;
                                        break;
                                    }
                                }
                            }
                            processo.diminuiRetorno();
                        }
                    } else if (processo.getRetorno() == 1) {
                        ArrayList<Integer> tabelaProcesso = processo.getMemoria();
                        int tamanhoTabela = tabelaProcesso.size();
                        int paginasNecessarias = 1;

                        if (paginasNecessarias > paginasPrincipalLivres) {
                            System.out.println("Memoria insuficiente para o processo " + processo.getPid());
                            processo.setEstado("Abortado");
                            executado.add(processo);
                            continue;
                        } else {
                            if (tamanhoTabela > 1) {
                                int npv = tabelaProcesso.get(1);
                                for (int npp = 0; npp < 16; npp++) {
                                    if (tpp[npp][1] == 0) {
                                        tpp[npp][1] = 1;
                                        tpp[npp][0] = npv;
                                        tpv[npv][0] = npp;
                                        tpv[npv][1] = 1;
                                        memoriaPrincipal[npp] = memoriaSecundaria[npv];
                                        paginasPrincipalLivres--;
                                        break;
                                    }
                                }
                            }
                            processo.diminuiRetorno();
                        }
                    }

                    processo.setEstado(Processo.EXEC);
                    processo.setHost(String.valueOf(hostID));
                    processo.setExecucao(Processo.agora());
                    executando.add(processo);

                    // envia processo para o host
                    outputStream.writeObject(processo);
                    outputStream.flush();

                    // aguarda o fim da execucao no host
                    if (inputStream.readBoolean()) {

                        executando.remove(processo);

                        if (quantum < processo.getResto()) {
                            processo.setResto(processo.getResto() - quantum);
                            fila.add(processo);
                            //System.out.println("[" + processo.getPid() + "] - " +
                            //        processo.getNome() + " retorna para a fila.");
                        } else {
                            processo.setResto(0);
                            //System.out.println("[" + processo.getPid() + "] - " +
                            //        processo.getNome() + " executado com sucesso.");
                            executado.add(processo);
                            processo.setEstado(Processo.FIM);
                        }

                        // Escreve processo no log
                        logger.trace(processo.toLog() + " | " + Processo.agora());
                    }
                }
                socket.close();
            } else {
                socket.close();
                throw new Exception("Não foi possível estabelecer handshake.");
            }
        } catch (InterruptedException e) {
            try {
                socket.close();
            } catch (IOException e1) {
                e1.printStackTrace();
            }
            System.out.println("Conexão com o host " + hostID + " encerrada.");
        } catch (Exception e) {
            System.out.println(e.getMessage());
            e.printStackTrace();
        }
    }
}