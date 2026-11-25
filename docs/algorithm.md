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

A ordem das somas em ponto flutuante também afeta os últimos bits. As futuras
versões paralelas devem definir uma ordem de redução e declarar a tolerância
numérica usada para comparar CPU e OpenCL. O cálculo sequencial usa a
recorrência de Welford para média e variância.
