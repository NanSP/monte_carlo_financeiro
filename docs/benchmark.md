# Comparação de desempenho

O comando mede três motores com os mesmos parâmetros: preço inicial 100,
exercício 100, taxa anual 5%, volatilidade anual 20%, prazo de um ano e
semente 42 por padrão. A entrada sugerida na AV3 é de 100 milhões de
trajetórias; comece com um milhão para conferir a execução.

No Windows, com JDK 17 ou superior:

```powershell
.\scripts\benchmark.ps1 -Trajectories 1000000 -Workers 4 -Repetitions 5
```

Para a medição maior, altere `-Trajectories` para `100000000`. A saída é CSV;
ela pode ser redirecionada para um arquivo para compor a tabela do relatório.
O programa aquece os três motores com até 100 mil trajetórias, alterna a
ordem de execução entre repetições e usa a mediana dos tempos. Ele interrompe
a comparação se qualquer resultado diferir da referência sequencial.

`speedup = tempo_sequencial / tempo_paralelo`.
`efficiency = speedup / número_de_trabalhadores_ativos`.
O tempo inclui a criação e o encerramento do pool de threads e a redução
dos resultados. A mediana reduz o efeito de pausas ocasionais, mas não
elimina variação de JIT, coleta de lixo ou carga de outras aplicações.
Use a mesma máquina, JDK e número de repetições nas comparações apresentadas.
