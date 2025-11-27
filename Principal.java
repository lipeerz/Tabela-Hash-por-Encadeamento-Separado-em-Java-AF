import java.util.Random;
import java.util.Scanner;

/*
Classe Principal responsável por orquestrar os experimentos.
Define constantes de tamanho, seeds e datasets.
Gerencia o loop de testes e a impressão do CSV.
 */
public class Principal {

    public static final int[] TAMANHOS_M = {1009, 10007, 100003};
    public static final int QTD_TAMANHOS = 3;

    public static final int[] DATASETS_N = {1000, 10000, 100000};
    public static final int QTD_DATASETS = 3;

    public static final long[] SEEDS = {137, 271828, 314159};
    public static final int QTD_SEEDS = 3;

    public static final String[] NOMES_FUNC = {"H_DIV", "H_MUL", "H_FOLD"};
    public static final int QTD_FUNCS = 3;

    /*
    Método main.
    Imprime o cabeçalho do CSV e executa os laços aninhados para
    Cobrir todas as combinações de Tamanho x Função x Dataset.
     */
    public static void main(String[] args) {
        
        System.out.println("m,n,func,seed,ins_ms,coll_tbl,coll_lst,find_ms_hits,find_ms_misses,cmp_hits,cmp_misses,checksum");

        int i = 0;
        while (i < QTD_TAMANHOS) {
            int m = TAMANHOS_M[i];

            int j = 0;
            while (j < QTD_FUNCS) {
                int tipoHash = j;
                String nomeFunc = NOMES_FUNC[j];

                int k = 0;
                while (k < QTD_DATASETS) {
                    int n = DATASETS_N[k];
                    long seed = SEEDS[k];

                    executarExperimento(m, n, tipoHash, nomeFunc, seed);

                    k = k + 1;
                }
                j = j + 1;
            }
            i = i + 1;
        }
    }

    /*
    Executa um único experimento completo.
    Realiza a auditoria inicial, gera dados, executa aquecimento (JIT),
    Mede inserção, calcula checksum e mede busca (hits/misses).
    Imprime a linha resultante no formato CSV.
     */
    public static void executarExperimento(int m, int n, int tipoHash, String nomeFunc, long seed) {
        
        System.out.println("AUDITORIA: " + nomeFunc + " m=" + m + " seed=" + seed); 

        long[] dadosInsercao = gerarDados(n, seed);
        
        TabelaHash tabela = new TabelaHash(m, tipoHash);

        long lixo = 0;
        int aquecimento = 0;
        while (aquecimento < 1000) {
            if (tipoHash == 0) lixo = tabela.hashDivisao(aquecimento);
            if (tipoHash == 1) lixo = tabela.hashMultiplicacao(aquecimento);
            if (tipoHash == 2) lixo = tabela.hashDobramento(aquecimento);
            aquecimento = aquecimento + 1;
        }
        System.gc(); 

        long tempoInicio = System.nanoTime();
        
        long totalColisaoTabela = 0;
        long totalColisaoLista = 0;
        
        int idx = 0;
        while (idx < n) {
            long chave = dadosInsercao[idx];
            long[] res = tabela.inserir(chave);
            totalColisaoTabela = totalColisaoTabela + res[0];
            totalColisaoLista = totalColisaoLista + res[1];
            idx = idx + 1;
        }
        
        long tempoFim = System.nanoTime();
        double ins_ms = (tempoFim - tempoInicio) / 1000000.0;

        long checksum = tabela.getChecksum();

        int tamBusca = n;
        long[] chavesBusca = prepararLoteBusca(dadosInsercao, n, seed, tamBusca);

        double somaTempoHits = 0;
        double somaTempoMisses = 0;
        long totalCompHits = 0;
        long totalCompMisses = 0;
        long qtdHits = 0;
        long qtdMisses = 0;

        int repeticoes = 5;
        
        int aqB = 0;
        while (aqB < 100) {
           tabela.buscar(chavesBusca[aqB]); 
           aqB = aqB + 1;
        }

        int r = 0;
        while (r < repeticoes) {
            
            long tHitAcc = 0;
            long tMissAcc = 0;
            
            int b = 0;
            while (b < tamBusca) {
                long k = chavesBusca[b];
                
                long t0 = System.nanoTime();
                long comps = tabela.buscar(k);
                long t1 = System.nanoTime();
                
                if (comps > 0) {
                    tHitAcc = tHitAcc + (t1 - t0);
                    if (r == 0) {
                        totalCompHits = totalCompHits + comps;
                        qtdHits = qtdHits + 1;
                    }
                } else {
                    tMissAcc = tMissAcc + (t1 - t0);
                    if (r == 0) {
                        totalCompMisses = totalCompMisses + (-comps);
                        qtdMisses = qtdMisses + 1;
                    }
                }
                b = b + 1;
            }
            
            somaTempoHits = somaTempoHits + tHitAcc;
            somaTempoMisses = somaTempoMisses + tMissAcc;
            
            r = r + 1;
        }

        double find_ms_hits = (somaTempoHits / repeticoes) / 1000000.0;
        double find_ms_misses = (somaTempoMisses / repeticoes) / 1000000.0;
        
        double avg_cmp_hits = 0;
        if (qtdHits > 0) avg_cmp_hits = (double) totalCompHits / qtdHits;
        
        double avg_cmp_misses = 0;
        if (qtdMisses > 0) avg_cmp_misses = (double) totalCompMisses / qtdMisses;

        System.out.print(m + "," + n + "," + nomeFunc + "," + seed + ",");
        System.out.print(formatarDouble(ins_ms) + ",");
        System.out.print(totalColisaoTabela + ",");
        double mediaCollList = (double) totalColisaoLista / n;
        System.out.print(formatarDouble(mediaCollList) + ",");
        System.out.print(formatarDouble(find_ms_hits) + ",");
        System.out.print(formatarDouble(find_ms_misses) + ",");
        System.out.print(formatarDouble(avg_cmp_hits) + ",");
        System.out.print(formatarDouble(avg_cmp_misses) + ",");
        System.out.println(checksum);
    }

    /*
    Gera um vetor de números aleatórios long com base na seed fornecida.
    Utiliza aritmética simples para garantir o intervalo de 9 dígitos.
     */
    public static long[] gerarDados(int n, long seed) {
        Random rand = new Random(seed);
        long[] dados = new long[n];
        int i = 0;
        while (i < n) {
            int min = 100000000;
            int max = 999999999;
            long val = rand.nextInt(max - min + 1) + min;
            dados[i] = val;
            i = i + 1;
        }
        return dados;
    }

    /*
    Prepara o lote de busca contendo 50% de chaves presentes e 50% ausentes.
    Realiza o embaralhamento (shuffle) manual do vetor resultante.
     */
    public static long[] prepararLoteBusca(long[] dadosExistentes, int n, long seed, int tamBusca) {
        long[] lote = new long[tamBusca];
        Random rand = new Random(seed + 1);

        int qtdPresente = tamBusca / 2;
        
        int i = 0;
        while (i < qtdPresente) {
            int idxAleatorio = rand.nextInt(n);
            lote[i] = dadosExistentes[idxAleatorio];
            i = i + 1;
        }

        while (i < tamBusca) {
            int min = 100000000;
            int max = 999999999;
            long val = rand.nextInt(max - min + 1) + min;
            lote[i] = val;
            i = i + 1;
        }

        int j = 0;
        while (j < tamBusca) {
            int r = j + rand.nextInt(tamBusca - j);
            long temp = lote[r];
            lote[r] = lote[j];
            lote[j] = temp;
            j = j + 1;
        }

        return lote;
    }
    
    /*
    Formata um valor double para string com 4 casas decimais e separador ponto.
    Evita uso de bibliotecas de formatação que dependem de Locale.
     */
    public static String formatarDouble(double valor) {
        long temp = (long) (valor * 10000);
        double truncado = temp / 10000.0;
        return "" + truncado;
    }
}
