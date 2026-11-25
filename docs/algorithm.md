# Núcleo de precificação

O primeiro caso de uso é uma opção europeia de compra, sem dividendos. Cada
trajetória precisa apenas do preço no vencimento:

`S_T = S_0 * exp((r - sigma^2 / 2) * T + sigma * sqrt(T) * Z)`

`Z` segue a normal padrão. O pagamento descontado é
`exp(-r * T) * max(S_T - K, 0)`. A média dos pagamentos é a estimativa.
O intervalo de 95% usa `estimativa ± 1,959963984540054 * erro padrão`, com
variância amostral; o limite inferior é truncado em zero.

## Reprodutibilidade

Cada índice de trajetória determina seus próprios bits aleatórios com a semente:
`key = seed + 0x9E3779B97F4A7C15 * (index + 1)`, com aritmética `long` de 64 bits.
Dois passos SplitMix64 sobre `key` e `key + constante` geram uniformes no
intervalo aberto `(0, 1)`; Box-Muller produz `Z`. Threads, processos MPJ e
work-items OpenCL devem usar os mesmos índices e essa mesma regra.

As implementações Java processam blocos canônicos de 8192 trajetórias. A
versão sequencial percorre os blocos na ordem; a versão `STATIC` reparte faixas
de blocos entre as threads; a `DYNAMIC` entrega o próximo bloco livre a cada
thread. Cada bloco acumula média e variância com Welford. Ao final, os blocos
são combinados em ordem crescente de índice, dando o mesmo resultado Java
independentemente do escalonamento.

OpenCL pode usar funções matemáticas com arredondamento diferente. A futura
comparação com GPU precisará declarar uma tolerância numérica apropriada.
