//TODO Refatorar e transformar em um objeto, abandonando a função estática START()
package me.dliberalesso;

import java.util.*;
import java.util.concurrent.ArrayBlockingQueue;

public class Gerenciador {
    private static final String CABECALHO = " PID | NOME | TAMANHO |  CRIACAO | EXECUCAO |   STATUS   | HOST";
    private static List<Processo> executando = Collections.synchronizedList(new ArrayList<Processo>());
    private static ArrayBlockingQueue<Processo> fila = new ArrayBlockingQueue<>(100);
    private static int pid_processo = 1;

    public static void start() {
        new Thread(new Runnable() {
            @Override
            public void run() {
                entradas();
            }
        }).start();

        new Thread(new Runnable() {
            @Override
            public void run() {
                try {
                    worker(0);
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
            }
        }).start();

        new Thread(new Runnable() {
            @Override
            public void run() {
                try {
                    worker(1);
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
            }
        }).start();
    }

    private static void entradas() {
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
                fila.clear();
                fila.add(new Processo(9999, "", 0, ""));
                fila.add(new Processo(9999, "", 0, ""));
                break;
            } else {
                System.out.println("Comando invalido!");
            }
        }
    }

    private static void create(StringTokenizer st) {
        if (st.countTokens() != 3) {
            System.out.println("Comando invalido!");
        } else {
            String nome = st.nextToken();
            int tempo = Integer.parseInt(st.nextToken());
            String tamanho = st.nextToken() + "Kb";
            Processo processo = new Processo(pid_processo++, nome, tempo, tamanho);
            fila.add(processo);
        }
    }

    private static void ps() {
        System.out.println(CABECALHO);
        for (Processo p: executando) {
            System.out.println(p);
        }
        for (Processo p: fila) {
            System.out.println(p);
        }
    }

    private static void worker(int pid) throws InterruptedException {
        while (true) {
            Processo p = fila.take();
            if (p.getPid() == 9999) {
                break;
            } else {
                p.setEstado(Processo.EXEC);
                p.setHost(String.valueOf(pid));
                p.setExecucao(Processo.agora());
                executando.add(p);
                Thread.sleep(p.getTempo() * 1000);
                executando.remove(p);
            }
        }
    }
}
