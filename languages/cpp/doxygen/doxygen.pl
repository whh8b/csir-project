#!/usr/bin/perl

my $name, $package, $directory, $url, $db_host, $db_user, $db_pass, $db_db;
BEGIN {
$name = shift;
$package = shift;
$url = shift;
$directory = shift;
$db_host = shift;
$db_user = shift;
$db_pass = shift;
$db_db = shift;
}

#use strict;
use lib "$directory/perlmod";
use DoxyDocs;
use lib "./doxygen/";
use docdb;

$db = new docdb("localhost", "ir", "ir", "ir");
$db = new docdb($db_host, $db_user, $db_pass, $db_db);

if (!$db->connect()) {
	die "Not connected!\n";
}
print "Connected!\n";

#
# Create the package in the database.
#
$package_id = $db->add_package($name, $package, $url);

if ($package_id == -1) {
	die "Could not create the package.";
}

#
# Add empty source and insert documentation
#
foreach $class (@{$doxydocs->{classes}}) {
	#
	# Insert a class -- high level marker for 
	# dependency tracking.
	#
	#print "Class name: $class->{name}\n";
	$class_source_id = $db->add_source($package_id,
		"class",
		"",
		$class->{name},
		"");
	if ($class_source_id == -1) {
		print "Could not create class $class->{name} in docdb.\n";
		continue;
	}

	#
	# Look at each of the class' public methods.
	#
	foreach $method (@{$class->{public_methods}->{members}}) {
		$documentation = "";
		#
		# Collect the documentation for a method.
		#
		#print "Method name: $method->{name}\n";
		foreach (@{$method->{brief}->{doc}}) {
			if (ref($_) eq "HASH") {
				if ($_->{type} eq 'text') {
						$documentation .= $_->{content};
				}
			}
		}
		foreach (@{$method->{detailed}->{doc}}) {
			if (ref($_) eq "HASH") {
				$documentation .= $_->{content};
			}
		}
		$return_type = "";
		$return_type = $method->{type} if defined $method->{type};
		$parameter_count = scalar @{$method->{parameters}};
		if ($documentation) {
			#print $documentation . "\n";
			$method_name = $class->{name} . "::" . $method->{name};
			print $method_name . "\n";
			$source_id = $db->add_source($package_id,
				"method",
				$return_type,
				$class->{name} . "::" . $method->{name},
				$parameter_count,
				"");
			if ($source_id != -1) {
				if (!$db->add_documentation($package_id, $source_id, $documentation)) {
					print "Failed to insert documentation for $method_name into docdb\n";	
				}
			} else {
				print "Failed to insert $method_name into docdb\n";
			}
			print "\n";
		}
	}
}
$db->disconnect();
