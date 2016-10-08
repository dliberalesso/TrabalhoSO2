package me.dliberalesso;

import java.io.PrintWriter;
import java.io.FileWriter;
import java.io.File;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.*;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

public class Gerenciador implements Runnable{
    private ServerSocket serverSocket;
    private ExecutorService pool;
    private int poolSize;

    private String CABECALHO = " PID | NOME | TAMANHO |  CRIACAO | EXECUCAO |   STATUS   | HOST";
    private List<Processo> executando = Collections.synchronizedList(new ArrayList<Processo>());
    private ArrayBlockingQueue<Processo> fila = new ArrayBlockingQueue<>(100);
    private int pid = 1;
    private int physicalPages = 16;
    private int virtualPages = 32;
    private static final int pageSize = 8;
    private String exitTxt;

    public Gerenciador(int porta, int poolSize) throws IOException {
        this.serverSocket = new ServerSocket(porta);
        this.pool = Executors.newFixedThreadPool(poolSize);
        this.poolSize = poolSize;
        exitTxt = CABECALHO + "%n";
    }

    @Override
    public void run() {
        try {
            for (int i = 1; i <= poolSize; i++) {
                pool.execute(new ServerHandler(serverSocket.accept(), executando, fila, i));
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
                ps();
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

    private void ps() {
        System.out.println(CABECALHO);
        for (Processo p: executando) {
            System.out.println(p);
        }
        for (Processo p: fila) {
            System.out.println(p);
        }
    }

    private void create(StringTokenizer st) {
        if (st.countTokens() != 3) {
            System.out.println("Comando invalido!");
        } else {
            String nome = st.nextToken();
            int tempo = Integer.parseInt(st.nextToken());
            String tamanho = st.nextToken() + "Kb";
            Processo processo = new Processo(pid++, nome, tempo, tamanho);
            fila.add(processo);
            exitTxt += processo.toString() + "%n";
        }
    }

    private void exit() {
        pool.shutdown(); // Evita que novas tarefas sejam escalonadas
        fila.clear();
        //ps();
        try {
            // Aguarda 1 segundo para que tarefas terminem
            if (!pool.awaitTermination(1, TimeUnit.SECONDS)) {
                pool.shutdownNow(); // Cancela tarefas que ainda nao terminaram

                // Aguarda mais 15 segundos para que tarefas respondam ao sinal de terminar
                System.err.println("Aguardando a execução dos processos escalonados.");
            }
            FileWriter arquivo = new FileWriter(new File("resumo_execucao.txt"));
            PrintWriter printer = new PrintWriter(arquivo);
            printer.printf(exitTxt);
            arquivo.close();
        } catch (InterruptedException ie) {
            pool.shutdownNow();
            Thread.currentThread().interrupt();
        } catch (IOException e) {
            e.printStackTrace();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
    
    private void mem(StringTokenizer st) {
        if (st.countTokens() > 1) {
            System.out.println("Comando invalido!");
        } else if (st.nextToken().isEmpty()) {
            System.out.println("Tamanho em Bytes:\n"
                    + "Memória Física = " + pageSize * physicalPages + " B\n"
                    + "Memória Virtual = " + pageSize * virtualPages + " B\n");
        } else if (st.nextToken().equals("f")) {
            //code here
        } else if (st.nextToken().equals("v")) {
            //code here
        } else {
            System.out.println("Comando invalido!");
        }
    }
}

class ServerHandler implements Runnable {
    private Socket socket;
    private List<Processo> executando;
    private ArrayBlockingQueue<Processo> fila;
    private int hostID;

    private ObjectOutputStream outputStream;
    private ObjectInputStream inputStream;

    ServerHandler(Socket socket, List<Processo> executando, ArrayBlockingQueue<Processo> fila, int hostID) {
        this.socket = socket;
        this.executando = executando;
        this.fila = fila;
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
                while (!Thread.interrupted()) {
                    Processo processo = fila.take();
                    processo.setEstado(Processo.EXEC);
                    processo.setHost(String.valueOf(hostID));
                    processo.setExecucao(Processo.agora());
                    executando.add(processo);

                    // envia processo para o host
                    outputStream.writeObject(processo);
                    outputStream.flush();

                    // aguarda o fim da execucao no host
                    if (inputStream.readBoolean()) {
                        System.out.println("[" + processo.getPid() + "] - " +
                                processo.getNome() + " executado com sucesso.");
                        executando.remove(processo);
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