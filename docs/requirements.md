# Sistema de Gerenciamento de Colaboradores

## Objetivo
  Desenvolver um sistema responsável por centralizar todas as informações do colaborador, desde informações pessoais até informações profissionais, disponibilizando de forma fácil e com relatórios prontos para análise do responsável pelo setor de Gestão de Pessoas.

## Usuários do Sistema
  - Administrador
  - Gestor de Pessoas (Colaborador(es) responsável(eis) pela área)

## Problemas Identificados
  - Demora para localizar informações dos colaboradores
  - Controle manual das informações em diversas planilhas diferentes
  - Dificuldade para acompanhar o ciclo de vida do profissional dentro da empresa
  - Risco de perda de dados
  - Falta de validação/padronização nas informações espalhadas

## Requisitos Funcionais
  - RF01 - Cadastrar Gestor de Pessoas
  - RF02 - Editar Gestor de Pessoas
  - RF03 - Excluir Gestor de Pessoas
  - RF04 - Cadastrar Colaborador
  - RF05 - Editar Colaborador
  - RF06 - Inativar Colaborador
  - RF07 - Emitir Relatórios
  - RF08 - Admitir/Desligar Colaborador
  - RF09 - Afastar Colaborador
  - RF10 - Efetuar alterações em Mudanças de Cargos/Salário/Setor do Colaborador
  - RF11 - Histórico de Alterações do Colaborador (Timeline)

## Requisitos Não Funcionais
  - RFN01 - O sistema deve possuir autenticação
  - RFN02 - Apenas usuários autorizados poderão acessar determinadas funcionalidades
  - RFN03 - O sistema deve funcionar em dispositivos móveis (responsividade)
  - RFN04 - Os dados devem possuir backups periódicos
  - RFN05 - O tempo de resposta deve ser inferior à 2 segundos
  - RFN06 - O sistema deve estar em conformidade com a LGPD, garantindo consentimento, política de retenção e controle de acesso granular a dados pessoais
  - RFN07 - Criptografar dados sensíveis
  - RFN08 - O sistema deve garantir disponibilidade mínima de 99% (uptime), com RTO de X horas e RPO de Y horas

## Regras de Negócio
  - RN01 - Cada usuário do sistema deve possuir um email único
  - RN02 - Cada colaborador deve possuir um CPF único
  - RN03 - O sistema deve possuir logs de auditoria
  - RN04 - Colaboradores podem ser inativados por gestores, mas a exclusão definitiva (purga de dados) é restrita a administradores
  - RN05 - Um colaborador desligado não pode receber novas alterações de cargo/salário (RF10), apenas reativação
  - RN06 - Toda alteração de cargo/salário/setor (RF10) e todo afastamento (RF09) deve gerar registro automático na timeline (RF11)
  - RN07 - Não é permitido admitir um colaborador com CPF já ativo no sistema (complementa a RN02)
  
## Dúvidas para o Cliente
  1° - Quais campos exatos devem compor o cadastro pessoal e o profissional?

    -- Cadastro Pessoal
      - Nome Completo
      - RG
      - CPF
      - Data de Nascimento
      - Email Pessoal
      - Orientação Sexual / Gênero
      - Tipos de CNH
      - Quantidade de Dependentes
      - Telefone
      - Endereço
      -- Idioma(s)
        - Idioma
        - Grau de Fluência
      
    -- Cadastro Acadêmico
      - Grau de Formação
      -- Registro Acadêmico
        - Nome do Curso
        - Data de Inicio
        - Data de Término
        - Grau de Ensino
        - Ceritificado?
    
    -- Cadastro Profissional
      - Matrícula
      - Data de Admissão
      - Email Corporativo
      - Agência/Unidade
      -- Benefício escolhido
        - Nome do Benefício (Mimo: Uber etc etc)
      - Área de Atuação
      - Cargo/Função
      - Salário
      - Grade
      - Faixa
      - Gratificação
      - Carga Horária

    - Observações

  2° - Quais relatórios são realmente necessários no MVP e em qual formato de exportação?
  
    - Alterações em andamento
    - Desligamentos
    - Admissões
    - Faixa de idade dos colaboradores
    - Timeline de colaborador