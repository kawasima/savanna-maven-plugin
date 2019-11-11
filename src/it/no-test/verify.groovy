def buildLog = (String) new File("target/it/no-test/build.log").text
assert buildLog.contains("@t_wada")

