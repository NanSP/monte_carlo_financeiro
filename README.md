# monte_carlo_financeiro

Projeto da AV3 de Sistemas Distribuídos: precificação de uma opção europeia
de compra por simulação de Monte Carlo.

## Estado atual

O núcleo define parâmetros validados, amostras normais reproduzíveis por
`(semente, índice)`, preço analítico de Black–Scholes e estimativa com intervalo
de confiança de 95%. Há uma implementação sequencial e duas distribuições com
threads: faixas fixas (`STATIC`) e blocos distribuídos entre trabalhadores
(`DYNAMIC`). As três produzem o mesmo resultado para os mesmos parâmetros.
Consulte [o algoritmo](docs/algorithm.md).

O contrato `PricingEngine` será usado pelas futuras implementações MPJ e
OpenCL. As pastas `api`, `client` e `coordinator` estão reservadas para o
serviço distribuído com RMI e sockets TCP.

## Compilação e benchmark

Requer JDK 17 ou superior. O projeto contém um `pom.xml` para Maven, mas o
núcleo atual não depende de bibliotecas externas. A verificação independente
`br.uneb.montecarlo.FoundationCheck` compara as versões e é executada na
esteira de pull requests.

Para medir tempo, speedup e eficiência no Windows, execute
`.\scripts\benchmark.ps1 -Trajectories 1000000 -Workers 4 -Repetitions 5`.
O resultado é CSV; consulte [a metodologia](docs/benchmark.md).
