# monte_carlo_financeiro

Projeto da AV3 de Sistemas Distribuídos: precificação de uma opção europeia
de compra por simulação de Monte Carlo.

## Estado atual

O núcleo sequencial já define parâmetros validados, amostras normais
reproduzíveis por `(semente, índice)`, preço analítico de Black–Scholes,
estimativa de Monte Carlo e intervalo de confiança de 95%. O contrato
`PricingEngine` será compartilhado pelas implementações com threads, MPJ e
OpenCL. Consulte [o algoritmo](docs/algorithm.md).

As pastas `api`, `client` e `coordinator` estão reservadas para o serviço
distribuído com RMI e sockets TCP. As integrações MPJ e OpenCL ainda não foram
adicionadas.

## Compilação

Requer JDK 17 ou superior. O projeto contém um `pom.xml` para Maven, mas o
núcleo atual não depende de bibliotecas externas. O teste independente pode
ser compilado e executado apenas com `javac` e `java`.
