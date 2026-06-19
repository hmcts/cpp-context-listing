# Instructions for running the performance tests

1. The maven plugin for jmeter should handle most things and it is possible to run jmeter with:
	cd cpp.context.listing/listing-performance-test
	mvn clean verify -Plisting-performance-test
	
2. If you want to see and edit the jmeter tests you can launch Jmeter gui with:
	cd cpp.context.listing/listing-performance-test
	mvn jmeter:gui -Plisting-performance-test

3. The test results will be save in a .jtl file in ./target/jmeter/results/ 