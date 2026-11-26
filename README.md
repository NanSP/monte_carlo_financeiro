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

O [coordenador](docs/jobs.md) aceita tarefas de múltiplos clientes, limita
execuções e fila, publica estimativas parciais, registra falhas e permite
cancelamento. A camada RMI/TCP e os motores MPJ e OpenCL serão conectados em
etapas seguintes.

## Compilação e benchmark

Requer JDK 17 ou superior. O projeto contém um `pom.xml` para Maven, mas o
núcleo atual não depende de bibliotecas externas. `FoundationCheck` e
`CoordinatorCheck` são executados na esteira de pull requests.

Para medir tempo, speedup e eficiência no Windows, execute
`.\scripts\benchmark.ps1 -Trajectories 1000000 -Workers 4 -Repetitions 5`.
O resultado é CSV; consulte [a metodologia](docs/benchmark.md).
