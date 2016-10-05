package me.dliberalesso;

import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.net.InetAddress;
import java.net.Socket;

public class Host implements Runnable {
    private InetAddress hostname;
    private int porta;
    private int poolSize;

    public Host(InetAddress hostname, int porta, int poolSize) {
        this.hostname = hostname;
        this.porta = porta;
        this.poolSize = poolSize;
    }

    @Override
    public void run() {
        try {
            for (int i = 1; i <= poolSize; i++) {
                new Thread(new HostHandler(hostname, porta)).start();
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}

class HostHandler implements Runnable {
    private Socket socket;
    private ObjectOutputStream outputStream;
    private ObjectInputStream inputStream;
    private int hostID;

    public HostHandler(InetAddress hostname, int porta) throws IOException {
        this.socket = new Socket(hostname, porta);
        this.outputStream = new ObjectOutputStream(socket.getOutputStream());
        this.inputStream = new ObjectInputStream(socket.getInputStream());
    }

    @Override
    public void run() {
        try {
            // recebe o ID do host e confirma
            hostID = inputStream.readInt();

            outputStream.writeInt(hostID);
            outputStream.flush();

            // verifica se a conexao ainda esta ativa
            if (!socket.isClosed()) {
                System.out.println("Conectado como host " + hostID + ".");
                while (!Thread.interrupted()) {
                    Processo processo = (Processo)inputStream.readObject();
                    System.out.println("Host " + hostID + " execuntado processo [" + processo.getPid() + "] - " + processo.getNome() + ".");
                    Thread.sleep(processo.getTempo() * 1000);
                    outputStream.writeBoolean(true);
                    outputStream.flush();
                    System.out.println("[" + processo.getPid() + "] - " +
                            processo.getNome() + " executado com sucesso.");
                }
            }
        } catch (Exception e) {
            System.out.println("Conexão como host " + hostID + " encerrada.");
        }
    }
}
